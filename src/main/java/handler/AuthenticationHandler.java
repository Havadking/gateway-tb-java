package handler;

import io.netty.channel.ChannelHandlerContext;

public interface AuthenticationHandler {
    void authenticate(ChannelHandlerContext ctx, Object msg) throws Exception;
}
