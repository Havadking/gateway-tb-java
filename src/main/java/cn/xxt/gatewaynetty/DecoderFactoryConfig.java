package cn.xxt.gatewaynetty;

import cn.xxt.gatewaynetty.mqtt.builder.MqttMessageBuilderFactory;
import cn.xxt.gatewaynetty.mqtt.parser.MqttMessageParserFactory;
import cn.xxt.gatewaynetty.netty.protocol.ProtocolIdentifier;
import cn.xxt.gatewaynetty.netty.protocol.sender.TcpMessageNormalSender;
import cn.xxt.gatewaynetty.netty.protocol.sender.TcpMessageSenderFactory;
import cn.xxt.gatewaynetty.netty.protocol.sender.TcpMessageVideoSender;
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
    @Bean
    public TcpMessageSenderFactory tcpMessageSenderFactory() {
        // 注册默认的解析器
        return TcpMessageSenderFactory.createDefault();
    }

    @Bean
    public MqttMessageBuilderFactory mqttMessageBuilderFactory() {
        return MqttMessageBuilderFactory.createDefault();
    }

    @Bean
    public MqttMessageParserFactory mqttMessageParserFactory() {
        return MqttMessageParserFactory.createDefault();
    }
}
