package cn.xxt.gatewaynetty.netty.protocol.sender;

import cn.xxt.gatewaynetty.netty.model.DeviceData;
import io.netty.channel.Channel;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description:
 * @author: Havad
 * @create: 2025-02-15 09:21
 **/

public interface TcpMessageSender {
    /**
     * 向指定设备发送消息
     *
     * @param data    设备数据
     * @param channel 通道信息
     * @throws Exception 发送过程中遇到异常时抛出
     */
    void sendMessageToDevice(DeviceData data, Channel channel) throws Exception;
}
