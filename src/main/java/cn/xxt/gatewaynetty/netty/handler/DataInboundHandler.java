package cn.xxt.gatewaynetty.netty.handler;

import io.netty.channel.ChannelHandlerContext;

public interface DataInboundHandler {
    /**
     * 处理通道上下文和数据的方法
     *
     * @param ctx  通道上下文
     * @param data 数据对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    void handleData(ChannelHandlerContext ctx, Object data) throws Exception;
}
