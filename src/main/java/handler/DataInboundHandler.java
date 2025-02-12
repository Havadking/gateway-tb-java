package handler;

import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import lombok.AllArgsConstructor;
import model.DeviceData;
import util.LogUtil;
import util.PDUUtil;

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
     * 当从通道中读取到消息时被调用。
     *
     * <p>此方法首先通过PDUUtil的validateCheck方法验证接收到的消息，
     * 如果验证失败，则不处理该消息；
     * 如果验证成功，则将消息转换为DeviceData对象并将其放入Disruptor队列中，
     * 同时记录相应的日志信息。
     *
     * @param ctx 通道处理上下文
     * @param msg 从通道中读取到的消息对象
     * @throws Exception 当处理消息发生异常时抛出
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!PDUUtil.validateCheck((String) msg)){
            LogUtil.info(this.getClass().getName(), "channelRead", msg, "消息验证失败，丢弃该消息");
            return; // 直接返回，不再继续处理
        } {
            // 直接将Message放到Disruptor队列中
            LogUtil.info(this.getClass().getName(), "channelRead", msg, "数据写入Disruptor");
            DeviceData data = new DeviceData(PDUUtil.getDeviceNo((String) msg), (String) msg);
            producer.onData(data, DeviceDataEvent.Type.TO_TB);
        }
    }
}
