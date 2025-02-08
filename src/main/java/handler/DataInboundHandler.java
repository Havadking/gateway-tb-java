package handler;

import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import lombok.AllArgsConstructor;
import util.LogUtil;

/**
 * @program: gateway-netty
 * @description: 进行数据处理的Handler
 * @author: Havad
 * @create: 2025-02-08 17:33
 **/

@AllArgsConstructor
public class DataInboundHandler extends ChannelInboundHandlerAdapter {

    /**
     * 设备数据事件生产者
     */
    private final DeviceDataEventProducer producer;


    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward
     * to the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     * <p>
     * Sub-classes may override this method to change behavior.
     *
     * @param ctx 上下文
     * @param msg 数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 直接将Message放到Disruptor队列中
        LogUtil.info(this.getClass().getName(), "channelRead", msg, "数据写入Disruptor");
        producer.onData((String) msg, DeviceDataEvent.Type.TO_TB);
    }
}
