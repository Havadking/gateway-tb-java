package cn.xxt.gatewaynetty.netty.http;

import cn.xxt.gatewaynetty.netty.config.GatewayConfig;
import cn.xxt.gatewaynetty.netty.config.RedisConfig;
import cn.xxt.gatewaynetty.netty.handler.kar_video.face.HttpRequestHandler;
import cn.xxt.gatewaynetty.kafka.DeviceDataKafkaProducer;
import cn.xxt.gatewaynetty.netty.registry.DeviceRegistry;
import cn.xxt.gatewaynetty.netty.task.TaskManager;
import cn.xxt.gatewaynetty.util.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.springframework.stereotype.Component;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 用于卡尔视频话机下发人脸
 * @author: Havad
 * @create: 2025-02-17 14:51
 **/

@Component
public class HttpServer {
    /**
     * 设备注册信息
     */
    private final DeviceRegistry deviceRegistry;
    /**
     * 主事件循环组
     */
    private EventLoopGroup bossGroup;
    /**
     * 工作线程事件循环组
     */
    private EventLoopGroup workerGroup;
    /**
     * 通道
     */
    private Channel channel;
    /**
     * 任务管理器
     */
    private final TaskManager taskManager;
    /**
     * 设备数据事件生产者
     */
    private final DeviceDataKafkaProducer producer;

    public HttpServer(DeviceRegistry deviceRegistry, TaskManager taskManager, DeviceDataKafkaProducer producer) {
        this.deviceRegistry = deviceRegistry;
        this.taskManager = taskManager;
        this.producer = producer;
    }


    /**
     * 启动服务器方法。
     * <p>
     * 该方法负责初始化服务器，绑定端口并启动接受进来的连接。
     * 使用NIO传输和指定的Channel初始化ServerBootstrap。
     *
     * @throws Exception 如果在启动过程中发生错误，将抛出异常
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(65536),
                                    new HttpRequestHandler(deviceRegistry, producer, taskManager)
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(GatewayConfig.HTTP_PORT).sync();
            LogUtils.logBusiness("HTTP server started on port {}", GatewayConfig.HTTP_PORT);
            channel = f.channel();
//            f.channel().closeFuture().sync();

        } catch (Exception e) {
            // Exception here
            stop();
            throw e;
        }
    }

    /**
     * 停止服务方法
     * <p>
     * 此方法负责关闭通道、线程组，并释放相关资源。
     *
     * @see #channel
     * @see #bossGroup
     * @see #workerGroup
     * @see RedisConfig#closePool()
     */
    public void stop() {
        if (channel != null) {
            channel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        taskManager.shutdown();
        RedisConfig.closePool();
        LogUtils.logBusiness("HTTP Server stopped.");
    }
}


