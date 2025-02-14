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
    DeviceData parseMessage(MqttMessage message) throws Exception;
}
