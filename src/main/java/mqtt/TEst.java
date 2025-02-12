package mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * @program: gateway-netty
 * @description: 测试
 * @author: Havad
 * @create: 2025-02-08 14:56
 **/

public class TEst {
    public static void main(String[] args) throws MqttException {
        MqttConnection mqttConnection = new MqttConnection();
        MqttClient mqttClient = mqttConnection.getMqttClient();

        MqttSender mqttSender = new MqttSender(mqttClient);

        String deviceNo = "864603061185738";
        mqttSender.sendDeviceConnected(deviceNo);
        mqttSender.sendDeviceTelemetry("*#F#00551100011864603061185738   VER8.43 2024/05/280033");
//        mqttSender.sendDeviceDisconnected(deviceNo);

    }
}
