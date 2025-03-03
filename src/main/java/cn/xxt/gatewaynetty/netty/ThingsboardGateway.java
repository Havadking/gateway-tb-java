package cn.xxt.gatewaynetty.netty;

import cn.xxt.gatewaynetty.netty.handler.ProtocolDetectionHandler;
import cn.xxt.gatewaynetty.netty.videophone.http.HttpServer;
import cn.xxt.gatewaynetty.netty.videophone.http.file_tcp.FileTcpServer;
import cn.xxt.gatewaynetty.mqtt.MqttConnection;
import cn.xxt.gatewaynetty.mqtt.MqttReceiver;
import cn.xxt.gatewaynetty.mqtt.MqttSender;
import cn.xxt.gatewaynetty.mqtt.parser.MqttMessageParserFactory;
import cn.xxt.gatewaynetty.netty.protocol.ProtocolHandlerFactory;
import cn.xxt.gatewaynetty.netty.registry.DeviceRegistry;
import cn.xxt.gatewaynetty.netty.videophone.task.TaskManager;
import cn.xxt.gatewaynetty.util.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import cn.xxt.gatewaynetty.kafka.DeviceDataKafkaProducer;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 主程序
 * @author: Havad
 * @create: 2025-02-07 14:41
 **/

@Component
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class ThingsboardGateway {

    /**
     * Netty服务的端口号
     */
    @Value(value = "${netty.port}")
    private int port;
    /**
     * MQTT连接对象。
     */
    private final MqttConnection mqttConnection;
    /**
     * MQTT消息发送器
     */
    private final MqttSender mqttSender;
    /**
     * 设备注册表实例
     */
    private final DeviceRegistry deviceRegistry;
    /**
     * 设备数据Kafka生产者
     */
    private final DeviceDataKafkaProducer producer;
    /**
     * 任务管理器
     */
    private final TaskManager taskManager;
    /**
     * MQTT消息解析器工厂
     */
    private final MqttMessageParserFactory parserFactory;
    /**
     * HTTP服务器实例。
     */
    private final HttpServer httpServer;
    /**
     * 文件TCP服务器实例。
     */
    private final FileTcpServer fileTcpServer;

    /**
     * 主事件循环组
     */
    private EventLoopGroup bossGroup;
    /**
     * 工作线程事件循环组
     */
    private EventLoopGroup workerGroup;
    /**
     * 通道未来的状态和结果。
     */
    private ChannelFuture channelFuture;

    /**
     * 启动方法，负责初始化和启动整个系统。
     * 包括Disruptor、MQTT连接、事件处理器、HTTP服务器、TCP文件服务器以及Netty主线程。
     *
     * @throws Exception 启动过程中可能出现异常
     */
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void start() throws Exception {
        // 1. 获取MQTT客户端
        MqttClient mqttClient = mqttConnection.getMqttClient();

        ProtocolHandlerFactory handlerFactory = ProtocolHandlerFactory
                .createDefault(deviceRegistry, producer, mqttSender);

        // 2. 创建MQTT接收器
        MqttReceiver mqttReceiver = new MqttReceiver(producer, mqttClient, taskManager, parserFactory);

        // 3. 启动MQTT接收器
        mqttReceiver.start();

        // 4. 启动HTTP服务器
        httpServer.start();

        // 5. 启动TCP文件服务器
        fileTcpServer.start();

        // 6. 启动Netty服务器
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();


        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new ProtocolDetectionHandler(handlerFactory)
                            );
                        }
                    });

            channelFuture = b.bind(port).sync();
            LogUtils.logBusiness("Gateway server started on port {}", port);
            channelFuture.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    /**
     * 关闭网关
     * 当Spring应用关闭时自动调用此方法
     */
    @PreDestroy
    public void shutdown() {
        LogUtils.logBusiness("Shutting down gateway...");

        // 关闭Netty资源
        if (channelFuture != null) {
            channelFuture.channel().close();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        LogUtils.logBusiness("Gateway shutdown complete");
    }
}
