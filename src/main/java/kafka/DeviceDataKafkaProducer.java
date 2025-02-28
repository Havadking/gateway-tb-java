package kafka;

import disruptor.DeviceDataEvent;
import lombok.AllArgsConstructor;
import model.DeviceData;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import util.LogUtils;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-27 15:23
 **/
@Component
@AllArgsConstructor
public class DeviceDataKafkaProducer {
    /**
     * Kafka模板，用于发送设备数据
     */
    private final KafkaTemplate<String, DeviceData> kafkaTemplate;

    /**
     * 处理设备数据事件。
     * <p>
     * 当接收到设备数据时调用此方法，将数据发送到相应的Kafka主题。
     *
     * @param data 要发送的设备数据
     * @param type 事件类型（决定发送到哪个主题）
     */
    public void sendData(DeviceData data, DeviceDataEvent.Type type) {
        String topic;

        if (type == DeviceDataEvent.Type.TO_TB) {
            topic = KafkaTopics.TO_TB;
        } else if (type == DeviceDataEvent.Type.TO_DEVICE) {
            topic = KafkaTopics.TO_DEVICE;
        } else {
            throw new IllegalArgumentException("Unknown event type: " + type);
        }

        // 使用设备ID作为消息的key，确保同一设备的消息按顺序处理
        kafkaTemplate.send(topic, data.getDeviceId(), data);
        LogUtils.logBusiness("发送消息到Kafka主题[{}], 内容为{}", topic, data);
    }
}
