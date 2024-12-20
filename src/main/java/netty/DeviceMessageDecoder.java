package netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class DeviceMessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //打印接受到的数据
        System.out.println("接受到的数据：" + in.toString());
        if (in.readableBytes() < 4) {
            System.out.println("数据不足，等待更多数据");
            return; // 数据不足，等待更多数据
        }

        in.markReaderIndex(); // 标记读取位置

        int length = in.readInt(); // 读取消息长度
        if (in.readableBytes() < length) {
            System.out.println("剩余数据不足，等待更多数据");
            in.resetReaderIndex(); // 如果剩余数据不足，重置读取位置
            return;
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        // 将字节数组转换为业务对象
        DeviceMessage message = DeviceMessage.fromBytes(bytes);
        System.out.println("解析出来的数据：" + message);
        out.add(message);
    }
}
