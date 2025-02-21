package handler.kar_video;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import handler.DataInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import model.DeviceData;
import mqtt.MqttSender;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import protocol.ProtocolIdentifier;
import util.LogUtils;
import util.VideoParserUtil;

import java.nio.charset.StandardCharsets;
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
@AllArgsConstructor
public class DataInboundVideoHandler extends ChannelInboundHandlerAdapter implements DataInboundHandler {
    /**
     * 设备数据事件生产者
     */
    private final DeviceDataEventProducer producer;

    /**
     * MQTT消息发送器
     */
    private final MqttSender mqttSender;

    /**
     * 对象映射器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws JsonProcessingException {
        handleData(ctx, msg);
    }

    @Override
    public void handleData(ChannelHandlerContext ctx, Object data) throws JsonProcessingException {
        // 1. 提取所需要的值
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = (Map<String, Object>) data;
        // 获取 command
        String command = (String) messageMap.get("command");
        // 获取 request 内容
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) messageMap.get("request");
        if (request == null) {
            request = (Map<String, Object>) messageMap.get("response");
        }
        LogUtils.logBusiness("request is {}", request);
        if (command.equals("link")) {
            sendSuccessBack(ctx, ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).get());
        } else if (command.equals("devstatus")) {
            LogUtils.logBusiness("设备状态作为属性发送");
            String deviceId = ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).get();
            Map<String, Map<String, Object>> res = new HashMap<>();
            res.put(deviceId, request);
            // 转换为 JSON 字符串
            ObjectMapper mapper = new ObjectMapper();
            String payload = mapper.writeValueAsString(res);
            // 创建 MQTT 消息对象
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            LogUtils.logBusiness("发送设备属性的构建体为:{}", message);
            mqttSender.sendAttribute(message);
        } else {
            LogUtils.logBusiness("视频话机数据写入Disruptor:{}", data);
            DeviceData msg = new DeviceData(ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).get(),
                    data, ProtocolIdentifier.PROTOCOL_VIDEO);
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
        LogUtils.logBusiness("收到【link】请求，直接进行处理:{}", identity);
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
            // 发送数据
            VideoParserUtil.sendData(ctx.channel(), "link", jsonResponse);
        } catch (Exception e) {
            LogUtils.logError("构建认证响应失败", e);
            ctx.close();
        }
    }


    /**
     * 获取当前时间的方法
     *
     * @return 返回当前时间的字符串表示，格式为"yyyy-MM-dd HH:mm:ss"
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
