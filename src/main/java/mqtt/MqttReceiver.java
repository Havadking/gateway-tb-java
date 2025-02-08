package mqtt;

import disruptor.DeviceDataEventProducer;
import org.eclipse.paho.client.mqttv3.MqttClient;

/**
 * @program: gateway-netty
 * @description: 接收来自TB的信息
 * @author: Havad
 * @create: 2025-02-08 16:22
 **/

public class MqttReceiver {
    private final DeviceDataEventProducer producer;
    private final MqttClient mqttClient;

    public MqttReceiver(DeviceDataEventProducer producer, MqttClient mqttClient) {
        this.producer = producer;
        this.mqttClient = mqttClient;
    }
}
