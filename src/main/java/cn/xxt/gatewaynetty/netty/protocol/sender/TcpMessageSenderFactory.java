package cn.xxt.gatewaynetty.netty.protocol.sender;

import cn.xxt.gatewaynetty.netty.exceptions.UnsupportedProtocolException;
import cn.xxt.gatewaynetty.netty.protocol.ProtocolIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: tcp发送数据的工厂
 * @author: Havad
 * @create: 2025-02-15 09:43
 **/

public class TcpMessageSenderFactory {

    /**
     * 根据协议标识符映射的TCP消息发送器集合。
     */
    private final Map<ProtocolIdentifier, TcpMessageSender> senders = new HashMap<>();

    /**
     * 注册消息发送器
     *
     * @param protocolIdentifier 协议标识符
     * @param sender             消息发送器
     */
    public void registerSender(ProtocolIdentifier protocolIdentifier, TcpMessageSender sender) {
        senders.put(protocolIdentifier, sender);
    }

    /**
     * 根据协议类型获取TCP消息发送器
     *
     * @param protocolType 协议类型标识符
     * @return 对应协议类型的TCP消息发送器
     * @throws UnsupportedProtocolException 如果提供的协议类型不支持时抛出异常
     */
    public TcpMessageSender getSender(ProtocolIdentifier protocolType) {
        TcpMessageSender sender = senders.get(protocolType);
        if (sender == null) {
            throw new UnsupportedProtocolException(
                    String.format("Unsupported protocol type: %s", protocolType)
            );
        }
        return sender;
    }

    /**
     * 创建默认的TcpMessageSenderFactory实例
     *
     * @return 默认配置的TcpMessageSenderFactory对象
     */
    public static TcpMessageSenderFactory createDefault() {
        // 注册默认的解析器
        TcpMessageSenderFactory factory = new TcpMessageSenderFactory();
        factory.registerSender(ProtocolIdentifier.PROTOCOL_NORMAL, new TcpMessageNormalSender());
        factory.registerSender(ProtocolIdentifier.PROTOCOL_VIDEO, new TcpMessageVideoSender());
        return factory;
    }
}
