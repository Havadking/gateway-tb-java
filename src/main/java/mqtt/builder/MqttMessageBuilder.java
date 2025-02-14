package mqtt.builder;

import model.DeviceData;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @program: gateway-netty
 * @description: 用于处理不同协议的数据
 * @author: Havad
 * @create: 2025-02-14 15:21
 **/

public interface MqttMessageBuilder {
    MqttMessage buildMessage(DeviceData deviceData) throws Exception;
}
