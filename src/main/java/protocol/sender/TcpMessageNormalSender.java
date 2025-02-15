package protocol.sender;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import model.DeviceData;

/**
 * @program: gateway-netty
 * @description: 适用于卡尔、掇月普通话机的发送
 * @author: Havad
 * @create: 2025-02-15 09:34
 **/

@Slf4j
public class TcpMessageNormalSender implements TcpMessageSender {
    @Override
    public void sendMessageToDevice(DeviceData data, Channel channel) {
        log.info("【发往设备】【普通话机】数据为{}", data);
        // 普通话机，直接写回去即可，没有具体的字节协议
        if (channel != null && channel.isActive()) {
            try {
                // 从 Channel 的 ByteBufAllocator 分配 ByteBuf
                ByteBuf buf = channel.alloc().buffer();
                try {
                    // 写入数据
                    buf.writeBytes(data.serializeMsg());
                    // 写入并刷新 Channel
                    channel.writeAndFlush(buf);
                } catch (Exception e) {
                    // 确保在异常情况下释放 ByteBuf
                    buf.release();
                    throw e;
                }
            } catch (Exception e) {
                // 处理异常
                log.error("Error writing to channel", e);
            }
        }
    }
}
