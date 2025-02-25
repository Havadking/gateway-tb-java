import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import config.GatewayConfig;
import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventHandler;
import disruptor.DeviceDataEventProducer;
import handler.ProtocolDetectionHandler;
import http.HttpServer;
import http.file_tcp.FileTcpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import mqtt.MqttConnection;
import mqtt.MqttReceiver;
import mqtt.MqttSender;
import mqtt.builder.MqttMessageBuilderFactory;
import mqtt.parser.MqttMessageParserFactory;
import org.eclipse.paho.client.mqttv3.MqttClient;
import protocol.ProtocolHandlerFactory;
import protocol.sender.TcpMessageSenderFactory;
import registry.DeviceRegistry;
import task.TaskManager;
import util.LogUtils;

/**
 * @program: gateway-netty
 * @description: 主程序
 * @author: Havad
 * @create: 2025-02-07 14:41
 **/

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class ThingsboardGateway {
    @SuppressWarnings("checkstyle:RegexpSingleline")
    public static void main(String[] args) throws Exception {
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
//        disruptor.handleEventsWith(
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
