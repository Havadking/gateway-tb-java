package cn.xxt.gatewaynetty.netty.handler.kar_normal;

import cn.xxt.gatewaynetty.netty.handler.AuthenticationHandler;
import cn.xxt.gatewaynetty.netty.registry.DeviceRegistry;
import cn.xxt.gatewaynetty.util.AuthDeviceUtil;
import cn.xxt.gatewaynetty.util.LogUtils;
import cn.xxt.gatewaynetty.util.PDUUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 用于进行设备初次连接的认证
 * @author: Havad
 * @create: 2025-02-07 17:19
 **/

public class AuthenticationNormalHandler extends ChannelInboundHandlerAdapter implements AuthenticationHandler {

    /**
     * 设备注册信息
     */
    private final DeviceRegistry deviceRegistry;

    public AuthenticationNormalHandler(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LogUtils.logBusiness("卡尔普通话机读取第一条数据{}", msg);
        authenticate(ctx, msg);
    }

    @Override
    public void authenticate(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 验证数据合法性
        if (!PDUUtil.validateCheck((String) msg)) {
            LogUtils.logBusiness("卡尔普通话机协议格式验证失败：{}", msg);
            ctx.close();
        } else {
            LogUtils.logBusiness("卡尔普通话机协议格式验证成功：{}", msg);
            String deviceNo = PDUUtil.getDeviceNo((String) msg);
            if (AuthDeviceUtil.getDeviceAuth(deviceNo)) {
                LogUtils.logBusiness("卡尔普通话机认证成功:{}", deviceNo);
                deviceRegistry.register(deviceNo, ctx.channel());
                ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).set(deviceNo);
                //将消息传递到下一个Inbound
                ctx.fireChannelRead(msg);
                // 移除自身,避免多次认证
                ctx.pipeline().remove(this);
            } else {
                LogUtils.logBusiness("卡尔普通话机认证失败:{}", deviceNo);
                ctx.close();
            }
        }
    }
}
