package handler.kar_video;

import handler.AuthenticationHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import registry.DeviceRegistry;
import util.AuthDeviceUtil;
import util.LogUtils;

import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 卡尔视频话机认证
 * @author: Havad
 * @create: 2025-02-13 15:40
 **/

public class AuthenticationVideoHandler extends ChannelInboundHandlerAdapter implements AuthenticationHandler {

    /**
     * 设备注册表实例
     */
    private final DeviceRegistry deviceRegistry;

    public AuthenticationVideoHandler(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LogUtils.logBusiness("卡尔视频话机读取第一条数据{}", msg);
        authenticate(ctx, msg);
    }

    @Override
    public void authenticate(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 1. 提取所需要的值
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = (Map<String, Object>) msg;
        // 获取 command
        String command = (String) messageMap.get("command");
        // 获取 request 内容
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) messageMap.get("request");
        LogUtils.logBusiness("request is {}", request);
        // 获取具体字段
        String identity = (String) request.get("Identity");

        // 2. 检验是否为link
        if (!command.equals("link")) {
            LogUtils.logBusiness("卡尔视频话机协议格式验证失败<发送非link但验证>：{}", msg);
            ctx.close();
        } else {
            LogUtils.logBusiness("卡尔视频话机协议格式验证成功<发送link验证>：{}", msg);
            if (AuthDeviceUtil.getDeviceAuth(identity)) {
                LogUtils.logBusiness("卡尔视频话机认证成功：{}", identity);
                deviceRegistry.register(identity, ctx.channel());
                ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).set(identity);

                //将消息传递到下一个Inbound
                ctx.fireChannelRead(msg);
                // 移除自身,避免多次认证
                ctx.pipeline().remove(this);
            } else {
                LogUtils.logBusiness("卡尔视频话机认证失败：{}", identity);
                ctx.close();
            }
        }

    }
}
