package cn.xxt.gatewaynetty;

import cn.xxt.gatewaynetty.mqtt.MqttConnection;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description:
 * @author: Havad
 * @create: 2025-02-28 14:54
 **/

@Configuration
public class MqttConfig {
    /**
     * MQTT服务器的URL地址
     */
    @Value("${mqtt.url}")
    private String mqttUrl;

    /**
     * MQTT客户端ID
     */
    @Value("${mqtt.client-id}")
    private String mqttClientId;

    /**
     * MQTT用户名
     */
    @Value("${mqtt.username:}")
    private String mqttUsername;

    /**
     * 创建MQTT连接
     */
    @Bean
    public MqttConnection mqttConnection() {
        return new MqttConnection(mqttUrl, mqttClientId, mqttUsername);
    }

    /**
     * 创建并返回MQTT客户端。
     *
     * @param mqttConnection MQTT连接配置信息
     * @return MQTT客户端实例
     */
    @Bean
    public MqttClient mqttClient(MqttConnection mqttConnection) {
        return mqttConnection.getMqttClient();
    }
}
