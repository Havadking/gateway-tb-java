package handler;

import io.netty.channel.ChannelHandlerContext;

public interface AuthenticationHandler {
    /**
     * 对通道上下文和消息进行身份验证
     *
     * @param ctx 通道上下文对象
     * @param msg 需要验证的消息对象
     * @throws Exception 身份验证过程中出现异常时抛出
     */
    void authenticate(ChannelHandlerContext ctx, Object msg) throws Exception;
}
