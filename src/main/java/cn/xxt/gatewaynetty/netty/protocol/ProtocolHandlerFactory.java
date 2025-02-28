package cn.xxt.gatewaynetty.netty.protocol;

import cn.xxt.gatewaynetty.netty.exceptions.UnsupportedProtocolException;
import cn.xxt.gatewaynetty.netty.handler.kar_normal.AuthenticationNormalHandler;
import cn.xxt.gatewaynetty.netty.handler.kar_normal.DataInboundNormalHandler;
import cn.xxt.gatewaynetty.mqtt.MqttSender;
import cn.xxt.gatewaynetty.netty.handler.kar_video.AuthenticationVideoHandler;
import cn.xxt.gatewaynetty.netty.handler.kar_video.DataInboundVideoHandler;
import cn.xxt.gatewaynetty.netty.handler.kar_video.JsonProtocolDecoder;
import cn.xxt.gatewaynetty.netty.registry.DeviceRegistry;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.string.StringDecoder;
import cn.xxt.gatewaynetty.kafka.DeviceDataKafkaProducer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 协议处理器工厂
 * @author: Havad
 * @create: 2025-02-13 10:32
 **/

public class ProtocolHandlerFactory {
    /**
     * 协议标识符到通道处理器工厂的映射
     */
    private final Map<ProtocolIdentifier, List<Supplier<ChannelHandler>>> handlerFactories = new HashMap<>();

    /**
     * 协议标识符到解码器工厂的映射
     */
    private final Map<ProtocolIdentifier, Supplier<ChannelHandler>> decoderFactories = new HashMap<>();

    /**
     * 注册协议处理器方法
     *
     * @param protocolId     协议标识符
     * @param handlerFactory 通道处理器工厂供应者
     */
    public void registerHandler(ProtocolIdentifier protocolId, Supplier<ChannelHandler> handlerFactory) {
        handlerFactories.computeIfAbsent(protocolId, k -> new ArrayList<>()).add(handlerFactory);
    }

    /**
     * 注册解码器
     *
     * @param protocolId     协议标识符
     * @param decoderFactory 解码器工厂
     */
    public void registerDecoder(ProtocolIdentifier protocolId, Supplier<ChannelHandler> decoderFactory) {
        decoderFactories.put(protocolId, decoderFactory);
    }


    /**
     * 根据协议标识获取对应的通道处理器
     *
     * @param protocolId 协议标识符
     * @return 对应协议的通道处理器
     * @throws Exception 当无法获取对应的通道处理器时抛出异常
     */
    public List<ChannelHandler> getHandlers(ProtocolIdentifier protocolId) throws Exception {
        List<Supplier<ChannelHandler>> handlers = handlerFactories.get(protocolId);
        if (handlers == null) {
            throw new UnsupportedProtocolException("获取handler失败:" + protocolId);
        }
        return handlers.stream()
                .map(Supplier::get)
                .collect(Collectors.toList());
    }

    /**
     * 根据协议标识获取解码器
     *
     * @param protocolId 协议标识
     * @return 对应协议的解码器
     * @throws Exception 如果根据协议标识获取解码器失败
     */
    public ChannelHandler getDecoder(ProtocolIdentifier protocolId) throws Exception {
        // 获取并添加解码器
        Supplier<ChannelHandler> decoderFactory = decoderFactories.get(protocolId);
        if (decoderFactory == null) {
            throw new Exception("获取decoder失败: " + protocolId);
        }
        return decoderFactory.get();
    }

    /**
     * 创建默认的协议处理器工厂。
     *
     * @param deviceRegistry 设备注册中心
     * @param producer       设备数据事件生产者
     * @param sender         MQTT消息发送者
     * @return 创建的默认协议处理器工厂实例
     */
    public static ProtocolHandlerFactory createDefault(DeviceRegistry deviceRegistry,
                                                       DeviceDataKafkaProducer producer, MqttSender sender) {
        ProtocolHandlerFactory factory = new ProtocolHandlerFactory();

        // 注册普通话机的协议的 gateway.handler
        factory.registerHandler(ProtocolIdentifier.PROTOCOL_NORMAL,
                () -> new AuthenticationNormalHandler(deviceRegistry));
        factory.registerHandler(ProtocolIdentifier.PROTOCOL_NORMAL,
                () -> new DataInboundNormalHandler(producer));
        factory.registerDecoder(ProtocolIdentifier.PROTOCOL_NORMAL, StringDecoder::new);

        // 注册视频话机的协议的 gateway.handler
        factory.registerHandler(ProtocolIdentifier.PROTOCOL_VIDEO,
                () -> new AuthenticationVideoHandler(deviceRegistry));
        factory.registerHandler(ProtocolIdentifier.PROTOCOL_VIDEO,
                () -> new DataInboundVideoHandler(producer, sender));
        factory.registerDecoder(ProtocolIdentifier.PROTOCOL_VIDEO, JsonProtocolDecoder::new);

        return factory;
    }


}
