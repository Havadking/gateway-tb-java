package http.file_tcp;

import config.GatewayConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import util.LogUtils;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-21 17:37
 **/

public class FileTcpServer {

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
     * 启动网关服务器方法。
     * 该方法负责初始化服务器，绑定端口，并在服务器启动后同步关闭相关资源。
     *
     * @throws Exception 当初始化或绑定端口发生异常时抛出
     */
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
                                    new FileDownloadDecoder()
                            );
                        }
                    });

            ChannelFuture f = b.bind(GatewayConfig.FILE_PORT).sync();
            LogUtils.logBusiness("File server started on port {}", GatewayConfig.FILE_PORT);
            channel = f.channel(); // 保存channel引用
            // 不调用 f.channel().closeFuture().sync();

        } catch (Exception e) {
            stop();
            throw e;
        }
    }

    private void stop() {
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
