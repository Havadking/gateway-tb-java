package cn.xxt.gatewaynetty;

import cn.xxt.gatewaynetty.mqtt.builder.MqttMessageBuilderFactory;
import cn.xxt.gatewaynetty.mqtt.parser.MqttMessageParserFactory;
import cn.xxt.gatewaynetty.netty.protocol.sender.TcpMessageSenderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-28 16:22
 **/

@Configuration
public class DecoderFactoryConfig {
    /**
     * 创建并返回一个默认的TcpMessageSenderFactory实例。
     *
     * @return TcpMessageSenderFactory的默认实例
     */
    @Bean
    public TcpMessageSenderFactory tcpMessageSenderFactory() {
        // 注册默认的解析器
        return TcpMessageSenderFactory.createDefault();
    }

    /**
     * 创建默认的Mqtt消息构建工厂方法
     *
     * @return 默认配置的Mqtt消息构建工厂实例
     */
    @Bean
    public MqttMessageBuilderFactory mqttMessageBuilderFactory() {
        return MqttMessageBuilderFactory.createDefault();
    }

    /**
     * 创建默认的Mqtt消息解析器工厂
     *
     * @return 默认配置的Mqtt消息解析器工厂实例
     */
    @Bean
    public MqttMessageParserFactory mqttMessageParserFactory() {
        return MqttMessageParserFactory.createDefault();
    }
}
