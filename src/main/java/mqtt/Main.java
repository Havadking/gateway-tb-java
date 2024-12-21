package mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttMessage;


/**
 * @program: gateway-netty
 * @description: 测试
 * @author: Havad
 * @create: 2024-12-20 16:49
 **/

public class Main {
    public static void main(String[] args) {
        String brokerUrl = "tcp://192.168.9.230:1883";
        String clientId = "TestClient";
        String username = "test123";


        MQTTClient mqttClient = new MQTTClient(brokerUrl, clientId, username);


        try {
            // 1. 连接到 Broker
            mqttClient.connect();

            // 2. 声明设备
            String deviceJson = "{\"device\": \"" + "DeviceB" + "\"}";
            System.out.println(deviceJson);
            String topic = "v1/gateway/connect";
            mqttClient.publish(topic, deviceJson);


            // 3. 发送遥测测试
//            // 初始化 Jackson 的 ObjectMapper
//            ObjectMapper objectMapper = new ObjectMapper();
//            ObjectNode msg = objectMapper.createObjectNode();
//            // 创建 JSON 数组
//            ArrayNode nameArray = objectMapper.createArrayNode();
//            ObjectNode nameObject = objectMapper.createObjectNode();
//            nameObject.put("online", 1);
//            nameArray.add(nameObject);
//            msg.set("DeviceA", nameArray);
//            String jsonString = objectMapper.writeValueAsString(msg);
//            System.out.println(jsonString);
//            String topic_tele = "v1/gateway/telemetry";
//            mqttClient.publish(topic_tele, jsonString);




//            // 2. 发布测试消息
//            String topic = "v1/devices/me/telemetry";
//            String message = "Hello, MQTT with Auth!";
//            mqttClient.publish(topic, message);

            // 3. 断开连接
//            mqttClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
