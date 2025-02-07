import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * @program: gateway-netty
 * @description: 主程序
 * @author: Havad
 * @create: 2025-02-07 14:41
 **/

public class ThingsboardGateway {
    public static void main(String[] args) {
        // 0. 初始化 (配置、连接 MQTT Broker 等)
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();


    }
}
