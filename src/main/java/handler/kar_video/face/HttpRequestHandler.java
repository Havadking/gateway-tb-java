package handler.kar_video.face;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import model.DeviceData;
import protocol.ProtocolIdentifier;
import registry.DeviceRegistry;
import task.Task;
import task.TaskManager;
import util.LogUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 处理人脸下发和心跳
 * @author: Havad
 * @create: 2025-02-17 14:56
 **/
@AllArgsConstructor
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * 设备注册表实例
     */
    private final DeviceRegistry deviceRegistry;
    /**
     * 设备数据事件生产者
     */
    private final DeviceDataEventProducer producer;
    /**
     * 任务管理器
     */
    private final TaskManager taskManager;
    /**
     * 心跳检测接口URL
     */
    private static final String HEARTBEAT_URL = "/karface/cp/yf/heart.admin";
    /**
     * 对象映射器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 读取请求体中的JSON内容
        ByteBuf content = request.content();
        String jsonContent = content.toString(CharsetUtil.UTF_8);
        String deviceKey = getDeviceIdFromRequest(jsonContent);
        deviceRegistry.registerHttpChannel(deviceKey, ctx.channel());
        LogUtils.logBusiness("设备进行HTTP连接，还需要认证{}", deviceKey);
        // 1. 设备没有进行 TCP 链接，不进行 HTTP 连接
        if (deviceRegistry.getChannel(deviceKey) == null) {
            LogUtils.logBusiness("还没有建立TCP链接，关闭{}", deviceKey);
            ctx.close();
            return;
        }

        // 2. 处理设备发送的内容，现在只有心跳
        if (HEARTBEAT_URL.equals(request.uri())) {
            processHeartbeat(ctx, deviceKey, jsonContent);
        } else {
            ctx.close();
            LogUtils.logError("连接的uri不符合规定{}", new Throwable(), deviceKey);
        }
    }

    /**
     * 处理设备心跳信息
     *
     * @param ctx         通道处理器上下文
     * @param deviceKey   设备标识
     * @param jsonContent 心跳内容的JSON字符串
     * @throws JsonProcessingException 当JSON处理发生异常时抛出
     */
    private void processHeartbeat(ChannelHandlerContext ctx, String deviceKey, String jsonContent)
            throws JsonProcessingException {
        LogUtils.logBusiness("Received heartbeat from device {}: {}", deviceKey, jsonContent);

        // --- 1. 检查是否有回执 ---
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        JsonNode resultNode = rootNode.get("result");
        LogUtils.logBusiness("提取出的 result 值为 {}", resultNode);

        // 处理心跳信息（发送到 TB）
        processTaskResult(deviceKey, resultNode, jsonContent);


        // --- 2. 获取新的任务 ---
        Task task = taskManager.getNextTaskToProcess(deviceKey);

        // --- 3. 构建返回数据 ---
        Object responseContent;
        if (task != null) {
            Map<String, Object> taskResult = new HashMap<>();
            taskResult.put("personList", task.getPersonList());
            responseContent = taskResult;
            taskManager.markTaskSent(task.getTaskId());
            LogUtils.logBusiness("Sending task {} to device {}", task.getTaskId(), deviceKey);
        } else {
            Map<String, Object> simpleResult = new HashMap<>();
            simpleResult.put("result", 1);
            simpleResult.put("success", true);
            responseContent = simpleResult;
            LogUtils.logBusiness("No task to send to device {}", deviceKey);
        }

        String jsonResponse = objectMapper.writeValueAsString(
                Collections.singletonMap("result", responseContent));

        sendJsonResponse(ctx, jsonResponse);
    }

    /**
     * 发送JSON响应给客户端
     *
     * @param ctx          通道处理器上下文
     * @param jsonResponse JSON格式的响应字符串
     */
    private void sendJsonResponse(ChannelHandlerContext ctx, String jsonResponse) {
        // 不打印日志，base64 太长了 非常影响,看日志
//        LogUtils.logBusiness(" {}", jsonResponse);
        LogUtils.logBusiness("发送HTTP响应");
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(jsonResponse, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    LogUtils.logBusiness("响应发送成功:{}", HttpResponseStatus.OK.code());
                    // 成功处理逻辑
                    ctx.close();
                } else {
                    LogUtils.logError("响应发送失败: {}", future.cause(), future.cause().getMessage());
                    // 发生错误时关闭连接
                    ctx.close();
                }
            }
        });
    }

    /**
     * 处理任务结果，并根据需要更新心跳JSON
     *
     * @param deviceKey     设备标识
     * @param resultNode    结果节点
     * @param heartbeatJson 初始心跳JSON字符串
     */
    private void processTaskResult(String deviceKey, JsonNode resultNode, String heartbeatJson) {
        String finalHeartbeatJson = heartbeatJson;
        if (!resultNode.isEmpty()) {
            LogUtils.logBusiness("心跳中有消息回执");
            Task lastSentTask = taskManager.getLastSentTask(deviceKey);
            if (lastSentTask != null) {
                // 标记任务成功
                taskManager.markTaskSuccess(lastSentTask.getTaskId());

                // 将taskId添加到heartbeatJson中
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode heartbeatNode = mapper.readTree(heartbeatJson);

                    // 创建一个可修改的对象
                    ObjectNode mutableHeartbeat = (ObjectNode) heartbeatNode;

                    // 添加taskId字段
                    mutableHeartbeat.put("taskId", lastSentTask.getTaskId());

                    // 将修改后的JSON转换回字符串
                    finalHeartbeatJson = mapper.writeValueAsString(mutableHeartbeat);

                    LogUtils.logBusiness("Result for task {} (device {}): Heartbeat: {}",
                            lastSentTask.getTaskId(), deviceKey, finalHeartbeatJson);
                } catch (Exception e) {
                    LogUtils.logError("Failed to add taskId to heartbeat JSON", e);
                }
            } else {
                LogUtils.logBusiness("设备{}心跳有回执，但没找到上条任务", deviceKey);
            }
        } else {
            LogUtils.logBusiness("心跳中没有消息回执");
        }
        DeviceData msg = new DeviceData(deviceKey, finalHeartbeatJson, ProtocolIdentifier.PROTOCOL_VIDEO_FACE);
        producer.onData(msg, DeviceDataEvent.Type.TO_TB);
    }


    private String getDeviceIdFromRequest(String jsonContent) {
        try {
            // 使用Jackson解析JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonContent);
            String deviceKey = rootNode.path("deviceKey").asText();
            LogUtils.logBusiness("从JSON中获取deviceKey:{}", deviceKey);
            return deviceKey;
        } catch (Exception e) {
            LogUtils.logError("解析JSON获取deviceKey时出错", e);
            return null;
        }
    }

}
