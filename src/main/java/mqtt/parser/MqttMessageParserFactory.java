package mqtt.parser;

import protocol.ProtocolIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 解析器工厂
 * @author: Havad
 * @create: 2025-02-14 11:23
 **/

public class MqttMessageParserFactory {
    private final Map<ProtocolIdentifier, MqttMessageParser> parsers = new HashMap<>();

    public void registerParser(ProtocolIdentifier protocolType, MqttMessageParser parser) {
        parsers.put(protocolType, parser);
    }

    public MqttMessageParser getParser(ProtocolIdentifier protocolType) {
        return parsers.get(protocolType);
    }

    public static MqttMessageParserFactory createDefault() {
        // 注册默认的解析器
        MqttMessageParserFactory factory = new MqttMessageParserFactory();
        factory.registerParser(ProtocolIdentifier.PROTOCOL_NORMAL, new MqttMessageParserNormal());
        factory.registerParser(ProtocolIdentifier.PROTOCOL_VIDEO, new MqttMessageParserVideo());
        return factory;
    }
}
