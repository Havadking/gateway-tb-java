package mqtt;

import config.GatewayConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * @program: gateway-netty
 * @description: 管理MQTT连接
 * @author: Havad
 * @create: 2025-02-08 11:48
 **/

@Getter
@Slf4j
public class MqttConnection {
    /**
     * MQTT客户端实例。
     */
    private final MqttClient mqttClient;

    public MqttConnection() throws MqttException {
        // 1. 创建持久化对象
        // 使用内存持久化
        MemoryPersistence memoryPersistence = new MemoryPersistence();

        // 2. 创建 MQTT 客户端
        mqttClient = new MqttClient(
                GatewayConfig.MQTT_BROKER_URL,
                GatewayConfig.MQTT_CLIENT_ID,
                memoryPersistence
        );

        // 3. 配置连接选项
        // MQTT 连接选项
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // 设置为 true，表示每次连接都清除之前的会话
        // Thingsboard 需要用户名认证
        connOpts.setUserName(GatewayConfig.MQTT_USERNAME);

        // 4. 连接 MQTT Broker
        mqttClient.connect(connOpts);
        log.info("=====成功建立MQTT连接=====");
    }
}
