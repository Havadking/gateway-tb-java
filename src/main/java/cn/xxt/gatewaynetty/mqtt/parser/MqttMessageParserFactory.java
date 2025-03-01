package cn.xxt.gatewaynetty.mqtt.parser;

import cn.xxt.gatewaynetty.netty.exceptions.UnsupportedProtocolException;
import cn.xxt.gatewaynetty.netty.protocol.ProtocolIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 解析器工厂
 * @author: Havad
 * @create: 2025-02-14 11:23
 **/

public class MqttMessageParserFactory {
    /**
     * 协议标识符到MQTT消息解析器的映射集合。
     */
    private final Map<ProtocolIdentifier, MqttMessageParser> parsers = new HashMap<>();

    /**
     * 注册消息解析器
     *
     * @param protocolType 协议类型标识
     * @param parser       MQTT消息解析器
     */
    public void registerParser(ProtocolIdentifier protocolType, MqttMessageParser parser) {
        parsers.put(protocolType, parser);
    }

    /**
     * 根据协议类型获取MQTT消息解析器
     *
     * @param protocolType 协议类型标识
     * @return 对应协议类型的MQTT消息解析器，如果不存在则返回null
     */
    public MqttMessageParser getParser(ProtocolIdentifier protocolType) {
        MqttMessageParser parser = parsers.get(protocolType);
        if (parser == null) {
            throw new UnsupportedProtocolException(
                    String.format("Unsupported protocol type: %s", protocolType)
            );
        }
        return parser;
    }

    /**
     * 创建默认的Mqtt消息解析器工厂方法
     *
     * @return 默认配置的MqttMessageParserFactory实例
     */
    public static MqttMessageParserFactory createDefault() {
        // 注册默认的解析器
        MqttMessageParserFactory factory = new MqttMessageParserFactory();
        factory.registerParser(ProtocolIdentifier.PROTOCOL_NORMAL, new MqttNormalMessageParser());
        factory.registerParser(ProtocolIdentifier.PROTOCOL_VIDEO, new MqttVideoMessageParser());
        return factory;
    }
}
