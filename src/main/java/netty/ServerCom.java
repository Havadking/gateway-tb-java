package netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @program: gateway-netty
 * @description: 用来进行模拟普通话机的测试Server
 * @author: Havad
 * @create: 2025-01-20 17:01
 **/

public class ServerCom {

    // 用来保存收到的消息
    private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();



    public static void main(String[] args) {

        // 启动一个独立的线程来消费（打印）队列中的消息
        new Thread(() -> {
            while (true) {
                try {
                    // take()会阻塞直到队列里有数据
                    String msg = messageQueue.take();
                    System.out.println("从队列中取到数据并打印: " + msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();



        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(
                        new ChannelInitializer<NioSocketChannel>() {
                            @Override
                            protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                                nioSocketChannel.pipeline().addLast(new StringDecoder());
                                nioSocketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        System.out.println("收到客户端发来的数据: " + msg);
                                        messageQueue.put((String) msg);
                                    }
                                });
                            }
                        }
                )
                .bind(5566);
    }
}
