package mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * &#064;program:  gateway-netty
 * &#064;description:  MQTT Client Test
 * &#064;author:  Havad
 * &#064;create:  2024-12-20 16:49
 **/

public class Main {
    public static void main(String[] args) throws MqttException, JsonProcessingException {
        String brokerUrl = "tcp://192.168.9.230:1883";
        String clientId = "TestClient";
        String username = "test123";


        MQTTClient mqttClient = new MQTTClient(brokerUrl, clientId, username);
        // 1. 连接到 Broker
        mqttClient.connect();

        // Subscribe to the topic for RPC requests
        String rpcRequestTopic = "v1/gateway/rpc";
        mqttClient.subscribe(rpcRequestTopic, (topic, message) -> {
            System.out.println("Received message:");
            System.out.println("Topic: " + topic);
            String payload = new String(message.getPayload());
            System.out.println("Message: " + payload);

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode receivedJson = (ObjectNode) objectMapper.readTree(payload);

                String device = receivedJson.get("device").asText();
                int requestId = receivedJson.get("data").get("id").asInt();

                ObjectNode responseJson = objectMapper.createObjectNode();
                responseJson.put("device", device);
                responseJson.put("id", requestId);

                ObjectNode dataNode = objectMapper.createObjectNode();
                dataNode.put("success", true);
                responseJson.set("data", dataNode);

                String responseString = objectMapper.writeValueAsString(responseJson);
                mqttClient.publish(topic, responseString);

                System.out.println("Sent response: " + responseString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 2. 声明设备
//            String deviceJson = "{\"device\": \"" + "DeviceA" + "\"}";
//            System.out.println(deviceJson);
//            String topic_con = "v1/gateway/connect";
//            mqttClient.publish(topic_con, deviceJson);

//            String deviceDisJson = "{\"device\": \"" + "DeviceA" + "\"}";
//            System.out.println(deviceDisJson);
//            String topic_discon = "v1/gateway/disconnect`";
//            mqttClient.publish(topic_discon, deviceDisJson);


        // Initialize Jackson's ObjectMapper for JSON handling
        ObjectMapper objectMapper = new ObjectMapper();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Start a task to publish telemetry data every 30 seconds
        scheduler.scheduleAtFixedRate(new Runnable() {
            private int counter = 0; // Initialize the counter

            @Override
            public void run() {
                try {
                    // Create JSON object
                    ObjectNode msg = objectMapper.createObjectNode();
                    ArrayNode nameArray = objectMapper.createArrayNode();
                    ObjectNode nameObject = objectMapper.createObjectNode();
                    nameObject.put("online", counter++); // Increment counter
                    nameArray.add(nameObject);
                    msg.set("DeviceA", nameArray);

                    String jsonString = objectMapper.writeValueAsString(msg);
                    System.out.println("Publishing telemetry: " + jsonString);

                    String topic_tele = "v1/gateway/telemetry";
                    mqttClient.publish(topic_tele, jsonString);
                } catch (JsonProcessingException | MqttException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 30, TimeUnit.SECONDS);

        System.out.println("Listening for messages...");

        // Keep the main thread running
        synchronized (Main.class) {
            try {
                Main.class.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
