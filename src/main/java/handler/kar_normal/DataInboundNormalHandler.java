package handler.kar_normal;

import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import handler.DataInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.AllArgsConstructor;
import model.DeviceData;
import protocol.ProtocolIdentifier;
import util.LogUtils;
import util.PDUUtil;

/**
 * @program: gateway-netty
 * @description: 进行数据处理的Handler
 * @author: Havad
 * @create: 2025-02-08 17:33
 **/

@AllArgsConstructor
public class DataInboundNormalHandler extends ChannelInboundHandlerAdapter implements DataInboundHandler {

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
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        handleData(ctx, msg);
    }

    @Override
    public void handleData(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!PDUUtil.validateCheck((String) msg)) {
            LogUtils.logBusiness("普通话机消息验证失败，应该是话机上传短信{}", msg);
//            throw new Exception("消息验证失败");
            // 直接返回，不再继续处理
        } else  {
            // 直接将Message放到Disruptor队列中
            LogUtils.logBusiness("普通话机数据{}写入Disruptor", msg);
            DeviceData data = new DeviceData(
                    PDUUtil.getDeviceNo((String) msg),
                    msg,
                    ProtocolIdentifier.PROTOCOL_NORMAL);
            producer.onData(data, DeviceDataEvent.Type.TO_TB);
        }
    }
}
