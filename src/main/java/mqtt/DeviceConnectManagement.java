package mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @program: gateway-netty
 * @description: 设备连接和断连管理
 * @author: Havad
 * @create: 2024-12-20 17:27
 **/

public class DeviceConnectManagement {
    private static final String BROKER = "tcp://192.168.9.230:1883";
    private static final String USERNAME = "java";
    private static final String PASSWORD = "java";
    private static final String CLIENTID = "java";

    //名称后面会加上自动数字，例如：deviceA-1，数量取决于DEVICE_COUNT
    private static final String DEVICENAME = "deviceA-";
    private static final int DEVICE_COUNT = 2;


    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(BROKER, CLIENTID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
            client.connect(options);

            // 创建设备
            createDevices(client);

            // 关闭连接
            client.disconnect();
            System.out.println("=== 设备创建成功 ===");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void createDevices(MqttClient client) throws MqttException {
        for (int i = 0; i < DEVICE_COUNT; i++) {
            String deviceName = DEVICENAME + i;
            String deviceJson = "{\"device\": \"" + deviceName + "\"}";
            System.out.println(deviceJson);
            String topic = "v1/gateway/connect";
            MqttMessage message = new MqttMessage(deviceJson.getBytes());
            message.setQos(1);
            client.publish(topic, message);
            System.out.println("Created device: " + deviceName);
        }
    }

}
