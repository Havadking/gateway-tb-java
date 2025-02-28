package kafka;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import model.DeviceData;
import mqtt.MqttSender;
import mqtt.builder.MqttMessageBuilder;
import mqtt.builder.MqttMessageBuilderFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import protocol.sender.TcpMessageSender;
import protocol.sender.TcpMessageSenderFactory;
import registry.DeviceRegistry;
import util.LogUtils;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-27 15:24
 **/
@Component
@AllArgsConstructor
public class DeviceDataKafkaConsumer {
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
     * 处理发送到ThingsBoard的设备数据。
     *
     * @param deviceData 设备数据对象
     * @throws Exception 在处理过程中可能抛出的异常
     */
    @KafkaListener(topics = KafkaTopics.TO_TB, containerFactory = "deviceDataKafkaListenerContainerFactory")
    public void handleToThingsBoard(DeviceData deviceData) throws Exception {
        try {
            LogUtils.logBusiness("消费者处理发往TB的消息，数据为{}", deviceData.getMsg());

            // 1. 获取对应的信息构建器
            MqttMessageBuilder builder = builderFactory.getBuilder(deviceData.getProtocolType());
            // 2. 构建 MQTT 消息
            MqttMessage message = builder.buildMessage(deviceData);
            // 3. 发送 MQTT 信息
            mqttSender.sendToThingsboard(message);
        } catch (Exception e) {
            LogUtils.logBusiness("处理发往TB的消息失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 处理发送到设备的数据。
     *
     * @param deviceData 设备数据对象
     * @throws Exception 在处理过程中可能抛出的异常
     */
    @KafkaListener(topics = KafkaTopics.TO_DEVICE, containerFactory = "deviceDataKafkaListenerContainerFactory")
    public void handleToDevice(DeviceData deviceData) throws Exception {
        try {
            LogUtils.logBusiness("消费者处理发往设备的消息，数据为{}", deviceData.getMsg());

            // 1. 获取该数据流的 Channel
            Channel channel = deviceRegistry.getChannel(deviceData.getDeviceId());
            // 2. 获取对应的信息发送器
            TcpMessageSender sender = senderFactory.getSender(deviceData.getProtocolType());
            // 3. 发送数据到设备
            sender.sendMessageToDevice(deviceData, channel);
        } catch (Exception e) {
            LogUtils.logBusiness("处理发往设备的消息失败: {}", e.getMessage());
            throw e;
        }
    }
}
