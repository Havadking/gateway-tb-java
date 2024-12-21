package mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @program: gateway-netty
 * @description: MQTT客户端模块
 * @author: Havad
 * @create: 2024-12-20 16:30
 **/

public class MQTTClient {

    private final String brokerUrl;
    private final String clientId;
    private final String username;
    private MqttClient client;

    public MQTTClient(String brokerUrl, String clientId, String username) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.username = username;
    }

    // 连接到MQTT Broker
    public void connect() throws MqttException {
        if (client == null) {
            client = new MqttClient(brokerUrl, clientId);
        }

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);  // 清除会话
        options.setAutomaticReconnect(true);  // 自动重连
        options.setConnectionTimeout(10);  // 超时时间（秒）
        options.setUserName(username);

        // 设置回调函数
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                System.err.println("连接丢失: " + throwable.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println("收到消息: [主题] " + topic + " [内容] " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("消息发送完成: " + iMqttDeliveryToken.getMessageId());
            }
        });

        // 开始进行mqtt连接
        System.out.println("正在连接到 MQTT Broker...");
        client.connect(options);
        System.out.println("已连接到 MQTT Broker: " + brokerUrl);
    }

    // 发布消息
    public void publish(String topic, String message) throws MqttException {
        if (client == null || !client.isConnected()) {
            throw new IllegalStateException("客户端未连接");
        }

        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(1);  // 设置QoS级别
        client.publish(topic, mqttMessage);
        System.out.println("已发布消息: [主题] " + topic + " [内容] " + message);

    }

    // 断开连接
    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            System.out.println("已断开与 MQTT Broker 的连接");
        }
    }

    // 获取连接状态
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
