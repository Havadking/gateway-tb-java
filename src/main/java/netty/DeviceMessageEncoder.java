package netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class DeviceMessageEncoder extends MessageToByteEncoder<DeviceMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, DeviceMessage msg, ByteBuf out) throws Exception {
        System.out.println("开始进行编码数据：" + msg);

        byte[] bytes = msg.toBytes(); // 转换为字节数组
        out.writeInt(bytes.length); // 写入消息长度
        out.writeBytes(bytes); // 写入消息内容
    }
}