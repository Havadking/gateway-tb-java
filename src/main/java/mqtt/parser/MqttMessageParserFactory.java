package mqtt.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 解析器工厂
 * @author: Havad
 * @create: 2025-02-14 11:23
 **/

public class MqttMessageParserFactory {
    private final Map<String, MqttMessageParser> parsers = new HashMap<>();

    public void registerParser(String protocolType, MqttMessageParser parser) {
        parsers.put(protocolType, parser);
    }

    public MqttMessageParser getParser(String protocolType) {
        return parsers.get(protocolType);
    }

    public static MqttMessageParserFactory createDefault() {
        // 注册默认的解析器
        MqttMessageParserFactory factory = new MqttMessageParserFactory();
        factory.registerParser("NORMAL", new MqttMessageParserNormal());
        factory.registerParser("VIDEO", new MqttMessageParserVideo());
        return factory;
    }
}
