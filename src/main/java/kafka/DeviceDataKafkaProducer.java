package kafka;

import gataway.disruptor.DeviceDataEvent;
import gataway.model.DeviceData;
import gataway.util.LogUtils;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-27 15:23
 **/
@Component
@AllArgsConstructor
public class DeviceDataKafkaProducer {
    private final KafkaTemplate<String, DeviceData> kafkaTemplate;

    /**
     * 处理设备数据事件，发送到Kafka
     *
     * @param data 设备数据
     * @param type 事件类型
     */
    public void sendData(DeviceData data, DeviceDataEvent.Type type) {
        String topic;

        if (type == DeviceDataEvent.Type.TO_TB) {
            topic = KafkaConfig.TO_TB_TOPIC;
        } else if (type == DeviceDataEvent.Type.TO_DEVICE) {
            topic = KafkaConfig.TO_DEVICE_TOPIC;
        } else {
            throw new IllegalArgumentException("未知的事件类型: " + type);
        }

        // 使用设备ID作为消息的key，确保同一设备的消息按顺序处理
        ListenableFuture<SendResult<String, DeviceData>> future =
                kafkaTemplate.send(topic, data.getDeviceId(), data);

        // 添加回调处理发送结果
        future.addCallback(new ListenableFutureCallback<SendResult<String, DeviceData>>() {
            @Override
            public void onSuccess(SendResult<String, DeviceData> result) {
                LogUtils.logBusiness("发送消息到Kafka成功: topic={}, partition={}, offset={}, deviceId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        data.getDeviceId());
            }

            @Override
            public void onFailure(Throwable ex) {
                LogUtils.logBusiness("发送消息到Kafka失败: deviceId={}, error={}",
                        data.getDeviceId(), ex.getMessage());
            }
        });

        LogUtils.logBusiness("发送消息到Kafka主题[{}], 内容为{}", topic, data.getMsg());
    }
}
