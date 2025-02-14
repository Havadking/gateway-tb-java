package disruptor;

import com.lmax.disruptor.EventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mqtt.MqttSender;
import registry.DeviceRegistry;
import util.LogUtil;

/**
 * @program: gateway-netty
 * @description: Disruptor 事件处理器
 * @author: Havad
 * @create: 2025-02-08 17:15
 **/

@Slf4j
@AllArgsConstructor
public class DeviceDataEventHandler implements EventHandler<DeviceDataEvent> {

    /**
     * MQTT消息发送器
     */
    private final MqttSender mqttSender;

    /**
     * 设备注册实例
     */
    private final DeviceRegistry deviceRegistry;


    /**
     * 处理设备数据事件。
     * <p>
     * 当接收到设备数据事件时，根据事件类型将数据发送到Thingsboard或者设备，
     * 并记录相应的日志信息。
     *
     * @param event      设备数据事件对象
     * @param sequence   事件序列号
     * @param endOfBatch 标识是否为批处理结束的事件
     * @throws Exception 在处理事件过程中可能抛出的异常
     */
    @Override
    public void onEvent(DeviceDataEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.getType() == DeviceDataEvent.Type.TO_TB) {
            log.info("【发往thingsboard】消费了{}", sequence);
            // 由设备发往Thingsboard
            switch (event.getValue().getProtocolType()){
                case "NORMAL":
                    mqttSender.sendDeviceTelemetryProtocolNormal((String) event.getValue().getMsg());
                    break;
                case "VIDEO":
                    mqttSender.sendDeviceTelemetryProtocolVideo(event.getValue());
                    break;
                default:
                    // 需要如何处理？
                    break;
            }
            LogUtil.info(this.getClass().getName(), "onEvent", event.getValue().getMsg(), "由设备发往Thingsboard");
        } else if (event.getType() == DeviceDataEvent.Type.TO_DEVICE) {
            log.info("【发往设备】消费了{}", sequence);
            // 由Thingsboard发送到设备
            Channel channel = deviceRegistry.getChannel(event.getValue().getDeviceId());
            if (channel != null && channel.isActive()) {
                try {
                    // 从 Channel 的 ByteBufAllocator 分配 ByteBuf
                    ByteBuf buf = channel.alloc().buffer();
                    try {
                        // 写入数据
                        buf.writeBytes(event.getValue().serializeMsg());
                        // 写入并刷新 Channel
                        channel.writeAndFlush(buf);
                        LogUtil.info(this.getClass().getName(), "onEvent", event.getValue(), "由Thingsboard发送到设备");
                    } catch (Exception e) {
                        // 确保在异常情况下释放 ByteBuf
                        buf.release();
                        throw e;
                    }
                } catch (Exception e) {
                    // 处理异常
                    log.error("Error writing to channel", e);
                }
            }
        }
        event.clear();
    }
}
