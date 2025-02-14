package handler.kar_video;

import com.fasterxml.jackson.databind.ObjectMapper;
import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import handler.DataInboundHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.DeviceData;
import protocol.ProtocolIdentifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 卡尔视频话机数据处理
 * @author: Havad
 * @create: 2025-02-13 15:40
 **/
@Slf4j
@AllArgsConstructor
public class DataInboundHandlerVideo extends ChannelInboundHandlerAdapter implements DataInboundHandler {
    /**
     * 设备数据事件生产者
     */
    private final DeviceDataEventProducer producer;

    /**
     * 对象映射器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        handleData(ctx, msg);
    }

    @Override
    public void handleData(ChannelHandlerContext ctx, Object data) {
        // 1. 提取所需要的值
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = (Map<String, Object>) data;
        // 获取 command
        String command = (String) messageMap.get("command");
        // 获取 request 内容
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) messageMap.get("request");
        log.info("request is {}", request);
        // 获取具体字段
        String identity = (String) request.get("Identity");
        if (command.equals("link")) {
            sendSuccessBack(ctx, identity);
        } else {
            log.info("视频话机数据写入Disruptor:{}", data);
            DeviceData msg = new DeviceData(identity, data, ProtocolIdentifier.PROTOCOL_VIDEO);
            producer.onData(msg, DeviceDataEvent.Type.TO_TB);
        }
    }

    /**
     * 向客户端发送成功响应的私有方法
     *
     * @param ctx      通道处理器上下文
     * @param identity 设备标识
     */
    private void sendSuccessBack(ChannelHandlerContext ctx, String identity) {
        try {
            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("DeviceID", identity);
            response.put("DeviceManager", "xxt");
            response.put("Time", getCurrentTime());
            response.put("Result", "1");

            // 构建完整消息
            Map<String, Object> message = new HashMap<>();
            message.put("response", response);
            message.put("type", "terminal");
            message.put("command", "link");


            // 转换为JSON字符串
            String jsonResponse = objectMapper.writeValueAsString(message);

            // 发送响应
            ctx.writeAndFlush(Unpooled.copiedBuffer(jsonResponse.getBytes()))
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("认证响应发送成功{}", jsonResponse);
                        } else {
                            log.error("认证响应发送失败", future.cause());
                        }
                    });

        } catch (Exception e) {
            log.error("构建认证响应失败", e);
            ctx.close();
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
