package cn.xxt.gatewaynetty.mqtt;

import cn.xxt.gatewaynetty.util.LogUtils;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 管理MQTT连接
 * @author: Havad
 * @create: 2025-02-08 11:48
 **/
@Getter
public class MqttConnection {

    private final String mqttUrl;
    private final String clientId;
    private final String username;
    private MqttClient mqttClient;

    /**
     * 构造函数
     */
    public MqttConnection(String mqttUrl, String clientId, String username) {
        this.mqttUrl = mqttUrl;
        this.clientId = clientId;
        this.username = username;

        try {
            initMqttClient();
        } catch (Exception e) {
            throw new RuntimeException("初始化MQTT客户端失败", e);
        }
    }

    private void initMqttClient() throws MqttException {
        // 1. 创建持久化对象
        // 使用内存持久化
        MemoryPersistence memoryPersistence = new MemoryPersistence();

        // 2. 创建 MQTT 客户端
        mqttClient = new MqttClient(
                this.mqttUrl,
                this.clientId,
                memoryPersistence
        );

        // 3. 配置连接选项
        // MQTT 连接选项
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // 设置为 true，表示每次连接都清除之前的会话
        // Thingsboard 需要用户名认证
        connOpts.setUserName(this.username);
        // 默认值通常为10，可以根据需要调高
        connOpts.setMaxInflight(100);

        // 4. 连接 MQTT Broker
        mqttClient.connect(connOpts);
        LogUtils.logBusiness("=====网关1成功建立MQTT连接=====");
    }
}
