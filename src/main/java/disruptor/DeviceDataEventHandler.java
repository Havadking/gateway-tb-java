package disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import mqtt.MqttSender;
import mqtt.builder.MqttMessageBuilder;
import mqtt.builder.MqttMessageBuilderFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import protocol.sender.TcpMessageSender;
import protocol.sender.TcpMessageSenderFactory;
import registry.DeviceRegistry;
import util.LogUtils;

/**
 * @program: gateway-netty
 * @description: Disruptor 事件处理器
 * @author: Havad
 * @create: 2025-02-08 17:15
 **/

@AllArgsConstructor
public class DeviceDataEventHandler implements WorkHandler<DeviceDataEvent> {

    /**
     * MQTT消息发送器
     */
    private final MqttSender mqttSender;

    /**
     * 设备注册实例
     */
    private final DeviceRegistry deviceRegistry;

    /**
     * MQTT消息构造器工厂
     */
    private final MqttMessageBuilderFactory builderFactory;

    /**
     * TCP消息发送器工厂
     */
    private final TcpMessageSenderFactory senderFactory;


    /**
     * 处理设备数据事件。
     *
     * @param event      设备数据事件对象
     * @throws Exception 在处理事件过程中可能抛出的异常
     */
    @Override
    public void onEvent(DeviceDataEvent event) throws Exception {
        try {
            LogUtils.logBusiness("处理者[{}]消费了事件，数据为{}",
                    Thread.currentThread().getName(), event.getValue().getMsg());

            if (event.getType() == DeviceDataEvent.Type.TO_TB) {
                // 1. 获取对应的信息构建器
                MqttMessageBuilder builder = builderFactory.getBuilder(event.getValue().getProtocolType());
                // 2. 构建 MQTT 消息
                MqttMessage message = builder.buildMessage(event.getValue());
                // 3. 发送 MQTT 信息
                mqttSender.sendToThingsboard(message);
            } else if (event.getType() == DeviceDataEvent.Type.TO_DEVICE) {
                // 由Thingsboard发送到设备
                // 1. 获取该数据流的 Channel
                Channel channel = deviceRegistry.getChannel(event.getValue().getDeviceId());
                // 2. 获取对应的信息发送器
                TcpMessageSender sender = senderFactory.getSender(event.getValue().getProtocolType());
                // 3. 发送数据到设备
                sender.sendMessageToDevice(event.getValue(), channel);
            }
        } finally {
            // 确保在任何情况下都清理事件
            event.clear();
        }
    }
}
