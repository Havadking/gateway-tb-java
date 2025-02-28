package gataway;

import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import gataway.config.GatewayConfig;
import gataway.disruptor.DeviceDataEvent;
import gataway.disruptor.DeviceDataEventHandler;
import gataway.disruptor.DeviceDataEventProducer;
import gataway.handler.ProtocolDetectionHandler;
import gataway.http.HttpServer;
import gataway.http.file_tcp.FileTcpServer;
import gataway.mqtt.MqttConnection;
import gataway.mqtt.MqttReceiver;
import gataway.mqtt.MqttSender;
import gataway.mqtt.builder.MqttMessageBuilderFactory;
import gataway.mqtt.parser.MqttMessageParserFactory;
import gataway.protocol.ProtocolHandlerFactory;
import gataway.protocol.sender.TcpMessageSenderFactory;
import gataway.registry.DeviceRegistry;
import gataway.task.TaskManager;
import gataway.util.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.stereotype.Component;

/**
 * @program: gateway-netty
 * @description: 主程序
 * @author: Havad
 * @create: 2025-02-07 14:41
 **/

@Component
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class ThingsboardGateway {
    /**
     * 启动方法，负责初始化和启动整个系统。
     * 包括Disruptor、MQTT连接、事件处理器、HTTP服务器、TCP文件服务器以及Netty主线程。
     *
     * @throws Exception 启动过程中可能出现异常
     */
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public void start() throws Exception {
        // 1. 创建 Disruptor
        Disruptor<DeviceDataEvent> disruptor = new Disruptor<>(
                DeviceDataEvent::new,
                GatewayConfig.DISRUPTOR_BUFFER_SIZE,
                DaemonThreadFactory.INSTANCE, // 使用守护线程
                ProducerType.MULTI, // 多生产者模式
                new YieldingWaitStrategy() // 性能和CPU资源之间平衡
        );

        // 2. MQTT 连接
        MqttConnection mqttConnection = new MqttConnection();
        MqttClient mqttClient = mqttConnection.getMqttClient1();

        // 3. 创建组件
        MqttMessageParserFactory parserFactory = MqttMessageParserFactory.createDefault();
        MqttSender mqttSender = new MqttSender(mqttClient);
        DeviceRegistry deviceRegistry = new DeviceRegistry(mqttSender);
        DeviceDataEventProducer producer = new DeviceDataEventProducer(
                disruptor.getRingBuffer()
        );
        TaskManager taskManager = new TaskManager();
        MqttReceiver mqttReceiver = new MqttReceiver(producer, mqttClient, taskManager, parserFactory);

        // 4. 连接 Disruptor 的 Handler
        MqttMessageBuilderFactory builderFactory = MqttMessageBuilderFactory.createDefault();
        TcpMessageSenderFactory senderFactory = TcpMessageSenderFactory.createDefault();
//        gataway.disruptor.handleEventsWith(
//                new DeviceDataEventHandler(mqttSender, deviceRegistry, builderFactory, senderFactory)
//        );

        // 创建多个 EventHandler 实例
        DeviceDataEventHandler[] handlers = new DeviceDataEventHandler[GatewayConfig.DISRUPTOR_HANDLER_COUNT];
        for (int i = 0; i < GatewayConfig.DISRUPTOR_HANDLER_COUNT; i++) {
            handlers[i] = new DeviceDataEventHandler(mqttSender, deviceRegistry, builderFactory, senderFactory);
        }

        // 并行处理事件
        disruptor.handleEventsWithWorkerPool(handlers);

        // 5. 启动 Disruptor
        disruptor.start();

        // 6. 启动 MQTT 接收器
        mqttReceiver.start();

        // 7. 创建 ProtocolHandlerFactory
        ProtocolHandlerFactory handlerFactory =
                ProtocolHandlerFactory.createDefault(deviceRegistry, producer, mqttSender);

        // 8. 启动卡尔视频话机的 HTTP 服务器 和 TCP 文件服务器
        HttpServer httpServer = new HttpServer(deviceRegistry, taskManager, producer);
        httpServer.start();
        FileTcpServer fileTcpServer = new FileTcpServer();
        fileTcpServer.start();

        // 9. 启动 Netty 主线程
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
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

            ChannelFuture f = b.bind(GatewayConfig.PORT).sync();
            LogUtils.logBusiness("Gateway server started on port {}", GatewayConfig.PORT);
            f.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
