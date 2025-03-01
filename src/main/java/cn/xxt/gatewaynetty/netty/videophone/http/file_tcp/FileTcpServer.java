package cn.xxt.gatewaynetty.netty.videophone.http.file_tcp;

import cn.xxt.gatewaynetty.util.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description:
 * @author: Havad
 * @create: 2025-02-21 17:37
 **/

@Component
public class FileTcpServer {
    /**
     * Netty服务的端口号
     */
    @Value(value = "${netty.file_port}")
    private int port;

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

            ChannelFuture f = b.bind(port).sync();
            LogUtils.logBusiness("File server started on port {}", port);
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
