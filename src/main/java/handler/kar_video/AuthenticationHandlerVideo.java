package handler.kar_video;

import handler.AuthenticationHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import registry.DeviceRegistry;
import util.AuthDeviceUtil;
import util.LogUtil;

import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 卡尔视频话机认证
 * @author: Havad
 * @create: 2025-02-13 15:40
 **/

public class AuthenticationHandlerVideo extends ChannelInboundHandlerAdapter implements AuthenticationHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationHandlerVideo.class);
    public final DeviceRegistry deviceRegistry;

    public AuthenticationHandlerVideo(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LogUtil.info(this.getClass().getName(), "卡尔视频话机channelRead读取第一条数据", msg, "第一次读取");
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
            LogUtil.info(this.getClass().getName(), "卡尔视频话机channelRead验证第一条数据", msg, "发送非link但验证");
            ctx.close();
        } else {
            LogUtil.info(this.getClass().getName(), "卡尔视频话机channelRead验证第一条数据", msg, "发送link验证");
            if (AuthDeviceUtil.getDeviceAuth(identity)) {
                LogUtil.info(this.getClass().getName(), "卡尔视频话机channelRead开始进行认证", identity, "认证成功");
                deviceRegistry.register(identity, ctx.channel());
                ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).set(identity);
                //将消息传递到下一个Inbound
                ctx.fireChannelRead(msg);
                // 移除自身,避免多次认证
                ctx.pipeline().remove(this);
            } else {
                LogUtil.info(this.getClass().getName(), "卡尔视频话机channelRead开始进行认证", identity, "认证失败");
                ctx.close();
                // todo 是否需要做重试机制
            }
        }

    }
}
