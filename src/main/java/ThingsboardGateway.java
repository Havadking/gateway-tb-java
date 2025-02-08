import config.GatewayConfig;
import handler.AuthenticationHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import lombok.extern.slf4j.Slf4j;
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
        // 0. 初始化 (配置、连接 MQTT Broker 等)
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DeviceRegistry deviceRegistry = new DeviceRegistry();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    // 1. 将字节流转换成String类型
                                    new StringDecoder(),
                                    // 2. 初次连接时进行设备认证
                                    new AuthenticationHandler(deviceRegistry)
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
