package handler;

import config.GatewayConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocol.ProtocolHandlerFactory;
import protocol.ProtocolIdentifier;
import util.LogUtils;

import java.util.List;

/**
 * @program: gateway-netty
 * @description: 根据端口选择不同的 ChannelInitializer
 * @author: Havad
 * @create: 2025-02-13 11:22
 **/

@AllArgsConstructor
public class ProtocolDetectionHandler extends ChannelInboundHandlerAdapter {
    private static final byte PROTOCOL_A_HEADER = '*';  // 第一种协议以 '*' 开头
    private static final byte PROTOCOL_B_HEADER = '@';  // 第二种协议以 '@' 开头

    private final ProtocolHandlerFactory handlerFactory;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 1. 检测协议类型
        ProtocolIdentifier protocolId = detectProtocol(ctx, msg);
        LogUtils.logBusiness("信息{}对应的设备类型为:{}", msg, protocolId);
        // 2. 从工厂中获取对应 Handler
        // 获取所有 handlers
        List<ChannelHandler> handlers = handlerFactory.getHandlers(protocolId);
        // 获取特定类型的 handler
        AuthenticationHandler authHandler = handlers.stream()
                .filter(h -> h instanceof AuthenticationHandler)
                .map(h -> (AuthenticationHandler) h)
                .findFirst()
                .orElseThrow(() -> new Exception("Authentication handler not found"));

        DataInboundHandler dataHandler = handlers.stream()
                .filter(h -> h instanceof DataInboundHandler)
                .map(h -> (DataInboundHandler) h)
                .findFirst()
                .orElseThrow(() -> new Exception("Data handler not found"));

        // 3. 超时断连机制 Handler
        IdleStateHandler idleStateHandler = new IdleStateHandler(GatewayConfig.READ_TIME_OUT, 0, 0);
        IdleDisconnectHandler idleDisconnectHandler = new IdleDisconnectHandler();

        // 4. 将 Handler 添加到 Pipeline
        ctx.pipeline().addLast(handlerFactory.getDecoder(protocolId));
        ctx.pipeline().addLast((ChannelHandlerAdapter)authHandler);
        ctx.pipeline().addLast(idleStateHandler);
        ctx.pipeline().addLast(idleDisconnectHandler);
        ctx.pipeline().addLast((ChannelHandlerAdapter)dataHandler);

        // 5. 移除自身
        ctx.pipeline().remove(this);

        // 6. 触发后续 Handler 的 channelRead 事件
        ctx.fireChannelRead(msg);
    }

    /**
     * 检测协议标识符
     *
     * @param ctx 通道处理器上下文
     * @param msg 消息对象
     * @return 协议标识符，如果能够识别
     * @throws RuntimeException 当读取消息失败、消息不可读或无法识别协议时抛出
     */
    private ProtocolIdentifier detectProtocol(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            ctx.close();
            throw new RuntimeException("读取用户数据失败");
        }

        ByteBuf in = (ByteBuf) msg;
        if (!in.isReadable()) {
            throw new RuntimeException("判断协议类型时不可读");
        }

        // 读取第一个字节（不移动读指针）
        byte firstByte = in.getByte(in.readerIndex());
        ProtocolIdentifier protocolId = null;

        // 根据首字节判断协议类型
        if (firstByte == PROTOCOL_A_HEADER) {
            protocolId = ProtocolIdentifier.PROTOCOL_NORMAL;  // 对应 *#F# 开头的协议
        } else if (firstByte == PROTOCOL_B_HEADER) {
            protocolId = ProtocolIdentifier.PROTOCOL_VIDEO;  // 对应 @G 开头的 JSON 协议
        }

        if (protocolId == null) {
            // 无法识别的协议，关闭连接
            ctx.close();
            throw new RuntimeException("获取设备协议失败");
        }
        return protocolId;
    }
}
