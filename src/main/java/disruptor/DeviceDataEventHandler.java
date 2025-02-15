package disruptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mqtt.MqttSender;
import mqtt.builder.MqttMessageBuilder;
import mqtt.builder.MqttMessageBuilderFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import protocol.ProtocolIdentifier;
import protocol.sender.TcpMessageSender;
import protocol.sender.TcpMessageSenderFactory;
import registry.DeviceRegistry;
import util.VideoParserUtil;

import java.util.Map;

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
     * MQTT消息构造器工厂
     */
    private final MqttMessageBuilderFactory builderFactory;

    /**
     * TCP消息发送器工厂
     */
    private final TcpMessageSenderFactory senderFactory;


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
        log.info("消费了{}，数据为{}", sequence, event.getValue().getMsg());
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
        event.clear();
    }
}
