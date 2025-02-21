package mqtt.parser;

import model.DeviceData;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-14 11:14
 **/

public interface MqttMessageParser {
    /**
     * 解析MQTT消息为设备数据
     *
     * @param message MQTT消息对象
     * @return 解析后的设备数据
     * @throws Exception 解析过程中遇到异常时抛出
     */
    DeviceData parseMessage(MqttMessage message) throws Exception;
}
