package handler;

import io.netty.channel.ChannelHandlerContext;

public interface DataInboundHandler {
    void handleData(ChannelHandlerContext ctx, Object data) throws Exception;
}
