package cn.xxt.gatewaynetty.netty;

import cn.xxt.gatewaynetty.netty.config.GatewayConfig;
import cn.xxt.gatewaynetty.netty.handler.ProtocolDetectionHandler;
import cn.xxt.gatewaynetty.netty.http.HttpServer;
import cn.xxt.gatewaynetty.netty.http.file_tcp.FileTcpServer;
import cn.xxt.gatewaynetty.mqtt.MqttConnection;
import cn.xxt.gatewaynetty.mqtt.MqttReceiver;
import cn.xxt.gatewaynetty.mqtt.MqttSender;
import cn.xxt.gatewaynetty.mqtt.builder.MqttMessageBuilderFactory;
import cn.xxt.gatewaynetty.mqtt.parser.MqttMessageParserFactory;
import cn.xxt.gatewaynetty.netty.protocol.ProtocolHandlerFactory;
import cn.xxt.gatewaynetty.netty.protocol.sender.TcpMessageSenderFactory;
import cn.xxt.gatewaynetty.netty.registry.DeviceRegistry;
import cn.xxt.gatewaynetty.netty.task.TaskManager;
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
    // 自动注入所有需要的组件
    private final MqttConnection mqttConnection;
    private final MqttSender mqttSender;
    private final DeviceRegistry deviceRegistry;
    private final DeviceDataKafkaProducer producer;
    private final TaskManager taskManager;
    private final MqttMessageParserFactory parserFactory;
    private final HttpServer httpServer;
    private final FileTcpServer fileTcpServer;

    // Netty相关组件
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    /**
     * 启动方法，负责初始化和启动整个系统。
     * 包括Disruptor、MQTT连接、事件处理器、HTTP服务器、TCP文件服务器以及Netty主线程。
     *
     * @throws Exception 启动过程中可能出现异常
     */
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void start() throws Exception {






        // 1. 创建 Disruptor
//        Disruptor<DeviceDataEvent> disruptor = new Disruptor<>(
//                DeviceDataEvent::new,
//                GatewayConfig.DISRUPTOR_BUFFER_SIZE,
//                DaemonThreadFactory.INSTANCE, // 使用守护线程
//                ProducerType.MULTI, // 多生产者模式
//                new YieldingWaitStrategy() // 性能和CPU资源之间平衡
//        );

//        DeviceDataKafkaProducer producer = new DeviceDataKafkaProducer();
//
//
//        // 2. MQTT 连接
//        MqttConnection mqttConnection = new MqttConnection();
//        MqttClient mqttClient = mqttConnection.getMqttClient();
//
//        // 3. 创建组件
//        MqttMessageParserFactory parserFactory = MqttMessageParserFactory.createDefault();
//        MqttSender mqttSender = new MqttSender(mqttClient);
//        DeviceRegistry deviceRegistry = new DeviceRegistry(mqttSender);
////        DeviceDataEventProducer producer = new DeviceDataEventProducer(
////                disruptor.getRingBuffer()
////        );
//        TaskManager taskManager = new TaskManager();
//        MqttReceiver mqttReceiver = new MqttReceiver(producer, mqttClient, taskManager, parserFactory);
//
//        // 4. 连接 Disruptor 的 Handler
//        MqttMessageBuilderFactory builderFactory = MqttMessageBuilderFactory.createDefault();
//        TcpMessageSenderFactory senderFactory = TcpMessageSenderFactory.createDefault();
//        gataway.disruptor.handleEventsWith(
//                new DeviceDataEventHandler(mqttSender, deviceRegistry, builderFactory, senderFactory)
//        );

        // 创建多个 EventHandler 实例
//        DeviceDataEventHandler[] handlers = new DeviceDataEventHandler[GatewayConfig.DISRUPTOR_HANDLER_COUNT];
//        for (int i = 0; i < GatewayConfig.DISRUPTOR_HANDLER_COUNT; i++) {
//            handlers[i] = new DeviceDataEventHandler(mqttSender, deviceRegistry, builderFactory, senderFactory);
//        }
//
//        // 并行处理事件
//        disruptor.handleEventsWithWorkerPool(handlers);
//
//        // 5. 启动 Disruptor
//        disruptor.start();

        // 6. 启动 MQTT 接收器
//        mqttReceiver.start();
//
//        // 7. 创建 ProtocolHandlerFactory
//        ProtocolHandlerFactory handlerFactory =
//                ProtocolHandlerFactory.createDefault(deviceRegistry, producer, mqttSender);

        // 8. 启动卡尔视频话机的 HTTP 服务器 和 TCP 文件服务器
//        HttpServer httpServer = new HttpServer(deviceRegistry, taskManager, producer);
//        httpServer.start();
//        FileTcpServer fileTcpServer = new FileTcpServer();
//        fileTcpServer.start();

        // 9. 启动 Netty 主线程
//        EventLoopGroup bossGroup = new NioEventLoopGroup();
//        EventLoopGroup workerGroup = new NioEventLoopGroup();

        // 1. 获取MQTT客户端
        MqttClient mqttClient = mqttConnection.getMqttClient();

        ProtocolHandlerFactory handlerFactory = ProtocolHandlerFactory.createDefault(deviceRegistry, producer, mqttSender);

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

            channelFuture = b.bind(GatewayConfig.PORT).sync();
            LogUtils.logBusiness("Gateway server started on port {}", GatewayConfig.PORT);
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
