package handler.kar_video;

import handler.AuthenticationHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import registry.DeviceRegistry;
import util.AuthDeviceUtil;

import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 卡尔视频话机认证
 * @author: Havad
 * @create: 2025-02-13 15:40
 **/

@Slf4j
public class AuthenticationHandlerVideo extends ChannelInboundHandlerAdapter implements AuthenticationHandler {

    public final DeviceRegistry deviceRegistry;

    public AuthenticationHandlerVideo(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("卡尔视频话机读取第一条数据{}", msg);
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
        log.info("request is {}", request);
        // 获取具体字段
        String identity = (String) request.get("Identity");

        // 2. 检验是否为link
        if (!command.equals("link")) {
            log.info("卡尔视频话机协议格式验证失败<发送非link但验证>：{}", msg);
            ctx.close();
        } else {
            log.info("卡尔视频话机协议格式验证成功<发送link验证>：{}", msg);
            if (AuthDeviceUtil.getDeviceAuth(identity)) {
                log.info("卡尔视频话机认证成功：{}", identity);
                // todo 暂时注释掉用于测试
//                deviceRegistry.register(identity, ctx.channel());
                ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).set(identity);
                //将消息传递到下一个Inbound
                ctx.fireChannelRead(msg);
                // 移除自身,避免多次认证
                ctx.pipeline().remove(this);
            } else {
                log.info("卡尔视频话机认证失败：{}", identity);
                ctx.close();
                // todo 是否需要做重试机制
            }
        }

    }
}
