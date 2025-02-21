package protocol.sender;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import model.DeviceData;
import util.LogUtils;

/**
 * @program: gateway-netty
 * @description: 适用于卡尔、掇月普通话机的发送
 * @author: Havad
 * @create: 2025-02-15 09:34
 **/

public class TcpMessageNormalSender implements TcpMessageSender {

    @Override
    public void sendMessageToDevice(DeviceData data, Channel channel) {
        LogUtils.logBusiness("【发往设备】【普通话机】数据为{}", data);
        if (channel == null || !channel.isActive()) {
            LogUtils.logError("【发送失败】通道为空或未激活", new Throwable());
            return;
        }

        ByteBuf buf = null;
        try {
            // 从 Channel 的 ByteBufAllocator 分配 ByteBuf
            buf = channel.alloc().buffer();

            // 写入数据
            byte[] messageBytes = data.serializeMsg();
            buf.writeBytes(messageBytes);

            // 写入并刷新 Channel
            channel.writeAndFlush(buf).addListener(future -> {
                if (future.isSuccess()) {
                    LogUtils.logBusiness("【发送成功】消息已发送至设备，数据长度：{} 字节", messageBytes.length);
                } else {
                    LogUtils.logError("【发送失败】消息发送失败", future.cause());
                }
            });

            // 发送成功后 ByteBuf 会自动释放，不需要手动释放
            buf = null;

        } catch (Exception e) {
            LogUtils.logError("【发送失败】消息发送过程中发生异常", e);
            if (buf != null) {
                buf.release();
            }
        }
    }
}
