package netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: gateway-netty
 * @description: 用于处理连接发送的数据
 * @author: Havad
 * @create: 2025-01-20 17:52
 **/

@Slf4j
public class ClientRecHandler extends SimpleChannelInboundHandler<String> {
    // 在Handler构造函数里拿到外部传进来的channelGroup或其他集合
    private final ChannelGroup channelGroup;

    public ClientRecHandler(ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 在这里你可以先什么都不做，也可以记录一下正在建立连接的客户端地址
//        System.out.println("有新连接尝试进来: " + ctx.channel().remoteAddress());
        log.info("有新连接尝试进来: {}",ctx.channel().remoteAddress());
        super.channelActive(ctx);

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 收到客户端的第一个数据包后，对其进行解析
        if (isValid(msg)) {
            // 符合规则就将连接纳入管理
            channelGroup.add(ctx.channel());
            log.info("连接已加入管理集合:{} ", ctx.channel().id());
            // 这个 Handler 可以继续收后续的消息。此时你可以考虑修改 pipeline，
            // 或者保留本 Handler 根据需求来处理更多的数据。
            // 例如：ctx.pipeline().remove(this); // 如果你只关心首包
        } else {
            // 如果不符合规则，就关闭连接
            System.out.println("数据不符合规则，关闭连接: " + ctx.channel().remoteAddress());
            ctx.close();
        }
    }

    private boolean isValid(String msg) {
        // 你的验证逻辑
        return msg.startsWith("HELLO"); // 举个例子
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 如果连接断了，你可以做一些收尾处理，比如从集合里移除
        // 这里判断channelGroup里是否包含，也可以直接调用remove
        if (channelGroup.contains(ctx.channel())) {
            channelGroup.remove(ctx.channel());
            System.out.println("连接已断开并从管理集合移除: " + ctx.channel().id());
        }
        super.channelInactive(ctx);
    }
}
