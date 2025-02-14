package mqtt.parser;

import model.DeviceData;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @program: gateway-netty
 * @description: 用于卡尔视频话机的数据解析器
 * @author: Havad
 * @create: 2025-02-14 11:18
 **/

public class MqttMessageParserVideo implements MqttMessageParser {
    @Override
    public DeviceData parseMessage(MqttMessage message) throws Exception {
        return null;
    }
}
