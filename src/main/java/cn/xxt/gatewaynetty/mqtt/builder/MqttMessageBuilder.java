package cn.xxt.gatewaynetty.mqtt.builder;

import cn.xxt.gatewaynetty.netty.model.DeviceData;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 用于处理不同协议的数据
 * @author: Havad
 * @create: 2025-02-14 15:21
 **/

public interface MqttMessageBuilder {
    /**
     * 构建MQTT消息的方法
     *
     * @param deviceData 设备数据对象
     * @return 构建好的MQTT消息对象
     * @throws Exception 当构建消息过程中发生错误时抛出异常
     */
    MqttMessage buildMessage(DeviceData deviceData) throws Exception;
}
