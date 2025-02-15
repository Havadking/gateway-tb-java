package handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import registry.DeviceRegistry;
import util.LogUtils;

/**
 * @program: gateway-netty
 * @description: 处理空闲连接断开
 * @author: Havad
 * @create: 2025-02-12 14:16
 **/

@AllArgsConstructor
public class IdleDisconnectHandler extends ChannelInboundHandlerAdapter {

    /**
     * 当触发用户事件时调用的方法。
     *
     * <p>此方法在以下情况下被调用：
     * <ul>
     *     <li>IdleStateHandler 检测到读空闲超时</li>
     *     <li>IdleStateHandler 生成了一个 IdleStateEvent</li>
     *     <li>该事件通过 pipeline 传递到 IdleDisconnectHandler</li>
     * </ul>
     *
     * @param ctx ChannelHandlerContext对象
     * @param evt 触发的事件对象
     * @throws Exception 如果处理事件时发生异常
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                // 获取设备ID
                String deviceId = ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).get();
                LogUtils.logBusiness("【超时关闭】{}【超时关闭】", deviceId);
                // 关闭连接
                ctx.close();
            }
        }
    }
}
