package mqtt.builder;

import exceptions.UnsupportedProtocolException;
import protocol.ProtocolIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 数据构建器的工厂
 * @author: Havad
 * @create: 2025-02-14 15:25
 **/

public class MqttMessageBuilderFactory {
    /**
     * 协议标识符到MQTT消息构建器的映射。
     */
    private final Map<ProtocolIdentifier, MqttMessageBuilder> builders = new HashMap<>();

    /**
     * 注册协议构造器
     *
     * @param protocolType 协议类型标识
     * @param builder      MQTT消息构造器
     */
    public void registerBuilder(ProtocolIdentifier protocolType, MqttMessageBuilder builder) {
        builders.put(protocolType, builder);
    }

    /**
     * 根据协议标识获取Mqtt消息构建器
     *
     * @param protocolType 协议类型标识
     * @return 对应协议类型的Mqtt消息构建器
     */
    public MqttMessageBuilder getBuilder(ProtocolIdentifier protocolType) {
        MqttMessageBuilder builder = builders.get(protocolType);
        if (builder == null) {
            throw new UnsupportedProtocolException(
                    String.format("Unsupported protocol type: %s", protocolType)
            );
        }
        return builder;
    }

    /**
     * 创建默认的Mqtt消息构建工厂方法
     *
     * @return 默认配置的Mqtt消息构建工厂实例
     */
    public static MqttMessageBuilderFactory createDefault() {
        // 注册默认的解析器
        MqttMessageBuilderFactory factory = new MqttMessageBuilderFactory();
        factory.registerBuilder(ProtocolIdentifier.PROTOCOL_NORMAL, new MqttMessageNormalBuilder());
        factory.registerBuilder(ProtocolIdentifier.PROTOCOL_VIDEO, new MqttMessageVideoBuilder());
        factory.registerBuilder(ProtocolIdentifier.PROTOCOL_VIDEO_FACE, new MqttMessageVideoFaceBuilder());
        return factory;
    }
}
