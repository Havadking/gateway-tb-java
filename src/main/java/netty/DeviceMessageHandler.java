package netty;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class DeviceMessageHandler extends SimpleChannelInboundHandler<DeviceMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DeviceMessage msg) throws Exception {
        System.out.println("开始进行处理数据：" + msg);
        // 根据消息类型处理
        DeviceMessage response = null;
        switch (msg.getType()) {
            case "HEARTBEAT":
                System.out.println("收到心跳包：" + msg);
                response = new DeviceMessage("RESPONSE", "心跳已收到");
                break;
            case "CDR_UPLOAD":
                System.out.println("收到话单上传：" + msg);
                break;
            case "AUTH_REQUEST":
                System.out.println("收到号码认证请求：" + msg);
                break;
            default:
                System.out.println("未知消息类型：" + msg);
                response = new DeviceMessage("ERROR", "未知消息类型");
        }

        // 通过 writeAndFlush() 将响应发送给客户端
        System.out.println("发送响应的字节数: " + response.toBytes().length);
        DeviceMessage finalResponse = response;
        System.out.println("响应类型: " + response.getClass().getName());
        ctx.writeAndFlush(response).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("响应发送成功: " + finalResponse);
            } else {
                System.err.println("响应发送失败: " + future.cause());
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close(); // 关闭连接
    }
}
