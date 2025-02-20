package http;

import config.GatewayConfig;
import config.RedisConfig;
import disruptor.DeviceDataEventProducer;
import handler.kar_video.face.HttpRequestHandler;
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
import registry.DeviceRegistry;
import task.TaskManager;
import util.LogUtils;

/**
 * @program: gateway-netty
 * @description: 用于卡尔视频话机下发人脸
 * @author: Havad
 * @create: 2025-02-17 14:51
 **/

public class HttpServer {
    private final DeviceRegistry deviceRegistry;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;
    private final TaskManager taskManager;
    private final DeviceDataEventProducer producer;

    public HttpServer(DeviceRegistry deviceRegistry,TaskManager taskManager, DeviceDataEventProducer producer) {
        this.deviceRegistry = deviceRegistry;
        this.taskManager = taskManager;
        this.producer = producer;
    }


    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
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

        } catch (Exception e){
            // Exception here
            stop();
            throw e;
        }
    }

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

        RedisConfig.closePool();
        LogUtils.logBusiness("HTTP Server stopped.");
    }
}


