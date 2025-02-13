package protocol;

import disruptor.DeviceDataEventProducer;
import handler.normal.AuthenticationHandlerNormal;
import handler.normal.DataInboundHandlerNormal;
import io.netty.channel.ChannelHandler;
import registry.DeviceRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @program: gateway-netty
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
     * 注册协议处理器方法
     *
     * @param protocolId     协议标识符
     * @param handlerFactory 通道处理器工厂供应者
     */
    public void registerHandler(ProtocolIdentifier protocolId, Supplier<ChannelHandler> handlerFactory) {
        handlerFactories.computeIfAbsent(protocolId, k -> new ArrayList<>()).add(handlerFactory);
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
            throw new Exception("获取handler失败");
        }
        return handlers.stream()
                .map(Supplier::get)
                .collect(Collectors.toList());
    }

    /**
     * 创建默认的协议处理器工厂
     *
     * @param deviceRegistry 设备注册中心
     * @param producer       设备数据事件生产者
     * @return 创建的默认协议处理器工厂
     */
    public static ProtocolHandlerFactory createDefault(DeviceRegistry deviceRegistry, DeviceDataEventProducer producer) {
        ProtocolHandlerFactory factory = new ProtocolHandlerFactory();

        // 注册普通话机的协议的 handler
        factory.registerHandler(ProtocolIdentifier.PROTOCOL_NORMAL, () -> new AuthenticationHandlerNormal(deviceRegistry));
        factory.registerHandler(ProtocolIdentifier.PROTOCOL_NORMAL, () -> new DataInboundHandlerNormal(producer));

        // 注册视频话机的协议的 handler

        return factory;
    }


}
