import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import config.GatewayConfig;
import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventHandler;
import disruptor.DeviceDataEventProducer;
import handler.ProtocolDetectionHandler;
import handler.normal.AuthenticationHandlerNormal;
import handler.normal.DataInboundHandlerNormal;
import handler.IdleDisconnectHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import mqtt.MqttConnection;
import mqtt.MqttReceiver;
import mqtt.MqttSender;
import org.eclipse.paho.client.mqttv3.MqttClient;
import protocol.ProtocolHandlerFactory;
import registry.DeviceRegistry;

/**
 * @program: gateway-netty
 * @description: 主程序
 * @author: Havad
 * @create: 2025-02-07 14:41
 **/

@Slf4j
public class ThingsboardGateway {
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
        MqttClient mqttClient = mqttConnection.getMqttClient();

        // 3. 创建组件
        MqttSender mqttSender = new MqttSender(mqttClient);
        DeviceRegistry deviceRegistry = new DeviceRegistry(mqttSender);
        DeviceDataEventProducer producer = new DeviceDataEventProducer(
                disruptor.getRingBuffer()
        );
        MqttReceiver mqttReceiver = new MqttReceiver(producer, mqttClient);

        // 4. 连接 Disruptor 的 Handler
        disruptor.handleEventsWith(
                new DeviceDataEventHandler(mqttSender, deviceRegistry)
        );

        // 5. 启动 Disruptor
        disruptor.start();

        // 6. 启动 MQTT 接收器
        mqttReceiver.start();

        // 创建 ProtocolHandlerFactory
        ProtocolHandlerFactory handlerFactory = ProtocolHandlerFactory.createDefault(deviceRegistry, producer);

        // 7. Netty
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
                                    // 1. 将字节流转换成String类型
//                                    new StringDecoder(),
//                                    // 2. 初次连接时进行设备认证
//                                    new AuthenticationHandlerNormal(deviceRegistry),
//                                    // 3. 120 秒读空闲, 禁用写空闲和所有空闲
//                                    new IdleStateHandler(GatewayConfig.READ_TIME_OUT, 0, 0),
//                                    // 4. 处理空闲连接断开
//                                    new IdleDisconnectHandler(),
//                                    // 5. 将数据写到队列中
//                                    new DataInboundHandlerNormal(producer)
                                    new ProtocolDetectionHandler(handlerFactory)
                            );
                        }
                    });

            ChannelFuture f = b.bind(GatewayConfig.PORT).sync();
            log.info("Gateway server started on port {}", GatewayConfig.PORT);
            f.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
