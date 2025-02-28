package gataway.mqtt;

import gataway.config.GatewayConfig;
import gataway.util.LogUtils;
import lombok.Getter;
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
public class MqttConnection {
    /**
     * MQTT客户端实例。
     */
    private final MqttClient mqttClient1;

    /**
     * MQTT客户端实例2
     */
    private final MqttClient mqttClient2;

    @SuppressWarnings("checkstyle:MagicNumber")
    public MqttConnection() throws MqttException {
        // 1. 创建持久化对象
        // 使用内存持久化
        MemoryPersistence memoryPersistence = new MemoryPersistence();

        // 2. 创建 MQTT 客户端
        mqttClient1 = new MqttClient(
                GatewayConfig.MQTT_BROKER_URL,
                GatewayConfig.MQTT_CLIENT_ID,
                memoryPersistence
        );
        mqttClient2 = new MqttClient(
                GatewayConfig.MQTT_BROKER_URL,
                GatewayConfig.MQTT_CLIENT_ID + "2",
                memoryPersistence
        );

        // 3. 配置连接选项
        // MQTT 连接选项
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // 设置为 true，表示每次连接都清除之前的会话
        // Thingsboard 需要用户名认证
        connOpts.setUserName(GatewayConfig.MQTT_USERNAME);
        // 默认值通常为10，可以根据需要调高
        connOpts.setMaxInflight(100);
        MqttConnectOptions connOpts2 = new MqttConnectOptions();
        connOpts2.setCleanSession(true); // 设置为 true，表示每次连接都清除之前的会话
        // Thingsboard 需要用户名认证
        connOpts2.setUserName(GatewayConfig.MQTT_USERNAME_2);
        // 默认值通常为10，可以根据需要调高
        connOpts2.setMaxInflight(100);

        // 4. 连接 MQTT Broker
        mqttClient1.connect(connOpts);
        LogUtils.logBusiness("=====网关1成功建立MQTT连接=====");
        mqttClient2.connect(connOpts2);
        LogUtils.logBusiness("=====网关2成功建立MQTT连接=====");
    }
}
