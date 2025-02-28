package cn.xxt.gatewaynetty;

import cn.xxt.gatewaynetty.mqtt.MqttConnection;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
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
    @Value("${mqtt.url}")
    private String mqttUrl;

    @Value("${mqtt.client-id}")
    private String mqttClientId;

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
     * 创建MQTT客户端
     */
    @Bean
    public MqttClient mqttClient(MqttConnection mqttConnection) {
        return mqttConnection.getMqttClient();
    }
}
