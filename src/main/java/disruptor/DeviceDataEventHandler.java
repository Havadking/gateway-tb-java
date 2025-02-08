package disruptor;

import com.lmax.disruptor.EventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mqtt.MqttSender;
import registry.DeviceRegistry;
import util.LogUtils;
import util.PduUtil;

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



    @Override
    public void onEvent(DeviceDataEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.getType() == DeviceDataEvent.Type.TO_TB) {
            // 由设备发往Thingsboard
            mqttSender.sendDeviceTelemetry(event.getValue());
            LogUtils.info(this.getClass().getName(), "onEvent", event.getValue(), "由设备发往Thingsboard");
            log.info("消费了{}", sequence);
        } else if (event.getType() == DeviceDataEvent.Type.TO_DEVICE) {
            // 由Thingsboard发送到设备
            Channel channel = deviceRegistry.getChannel(PduUtil.getDeviceNo(event.getValue()));
            if (channel != null && channel.isActive()) {
                try {
                    // 从 Channel 的 ByteBufAllocator 分配 ByteBuf
                    ByteBuf buf = channel.alloc().buffer();
                    try {
                        // 写入数据
                        buf.writeBytes(event.getValue().getBytes(CharsetUtil.UTF_8));
                        // 写入并刷新 Channel
                        channel.writeAndFlush(buf);
                        LogUtils.info(this.getClass().getName(), "onEvent", event.getValue(), "由Thingsboard发送到设备");
                        log.info("消费了{}", sequence);
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
