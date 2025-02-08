package handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import registry.DeviceRegistry;
import util.AuthDeviceUtil;
import util.LogUtils;
import util.PduUtil;

/**
 * @program: gateway-netty
 * @description: 用于进行设备初次连接的认证
 * @author: Havad
 * @create: 2025-02-07 17:19
 **/

public class AuthenticationHandler extends ChannelInboundHandlerAdapter {

    public final DeviceRegistry deviceRegistry;

    public AuthenticationHandler(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LogUtils.info(this.getClass().getName(), "channelRead读取第一条数据", msg, "第一次读取");
        // 验证数据合法性
        if (!PduUtil.validateCheck((String) msg)) {
            LogUtils.info(this.getClass().getName(), "channelRead验证第一条数据", msg, "验证失败");
            ctx.close();
        } else {
            LogUtils.info(this.getClass().getName(), "channelRead验证第一条数据", msg, "验证成功");
            String deviceNo = PduUtil.getDeviceNo((String) msg);
            if (AuthDeviceUtil.getDeviceAuth(deviceNo)) {
                LogUtils.info(this.getClass().getName(), "channelRead开始进行认证", deviceNo, "认证成功");
                deviceRegistry.register(deviceNo, ctx.channel());
                ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).set(deviceNo);
                //将消息传递到下一个Inbound
                ctx.fireChannelRead(msg);
                // 移除自身
                ctx.pipeline().remove(this);

                System.out.println("暂停一下用于调试");
            } else {
                LogUtils.info(this.getClass().getName(), "channelRead开始进行认证", deviceNo, "认证失败");
                ctx.close();
                // todo 发送认证失败响应
            }
        }
    }
}
