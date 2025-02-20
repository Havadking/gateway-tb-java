package handler.kar_video.face;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import model.DeviceData;
import protocol.ProtocolIdentifier;
import registry.DeviceRegistry;
import task.Task;
import task.TaskManager;
import util.LogUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @program: gateway-netty
 * @description: 处理人脸下发和心跳
 * @author: Havad
 * @create: 2025-02-17 14:56
 **/
@AllArgsConstructor
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final DeviceRegistry deviceRegistry;
    private final DeviceDataEventProducer producer;
    private final TaskManager taskManager;
    private static final String HEARTBEAT_URL = "/karface/cp/yf/heart.admin";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
//        LogUtils.logBusiness("request is {}", request);
        LogUtils.logBusiness("uri is {}", request.uri());
        // 读取请求体中的JSON内容
        ByteBuf content = request.content();
        String jsonContent = content.toString(CharsetUtil.UTF_8);
        LogUtils.logBusiness("接收到的JSON内容: {}", jsonContent);

        String deviceKey = getDeviceIdFromRequest(jsonContent);
        LogUtils.logBusiness("设备ID为{}", deviceKey);

        deviceRegistry.registerHttpChannel(deviceKey, ctx.channel());

        LogUtils.logBusiness("设备进行HTTP连接，还需要认证{}", deviceKey);
        // 1. 设备没有进行 TCP 链接，不进行 HTTP 连接
        if (deviceRegistry.getChannel(deviceKey) == null) {
            LogUtils.logBusiness("还没有建立TCP链接，关闭{}", deviceKey);
            ctx.close();
            return;
        }

        // --- Handle the request (heartbeat or other) ---
        if (HEARTBEAT_URL.equals(request.uri())) {
            processHeartbeat(ctx, deviceKey, jsonContent);
        } else {
            ctx.close();
            LogUtils.logError("连接的uri不符合规定{}", new Throwable(), deviceKey);
        }
    }

    private void processHeartbeat(ChannelHandlerContext ctx, String deviceKey, String jsonContent) throws JsonProcessingException {
        LogUtils.logBusiness("Received heartbeat from device {}: {}", deviceKey, jsonContent);

        // --- 1. Check for previous task result ---
        JsonNode rootNode = objectMapper.readTree(jsonContent);
        JsonNode resultNode = rootNode.get("result");
        LogUtils.logBusiness("提取出的 result 值为 {}", resultNode);

        // 处理心跳信息
        processTaskResult(deviceKey, resultNode, jsonContent);


        // --- 2. Get a new task (if available) ---
        Task task = taskManager.getNextTaskToProcess(deviceKey);

        // --- 3. Build response ---
        Object responseContent;
        if (task != null) {
            Map<String, Object> taskResult = new HashMap<>();
            taskResult.put("photoList", task.getPersonList());
            responseContent = taskResult;
            taskManager.markTaskSent(task.getTaskId()); // Mark as sent
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

        sendJsonResponse(ctx, OK, jsonResponse);
    }

    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String jsonResponse) {
        LogUtils.logBusiness("下发任务:{}", jsonResponse);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(jsonResponse, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    LogUtils.logBusiness("响应发送成功:{}", status.code());
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

    private void processTaskResult(String deviceKey, JsonNode resultNode, String heartbeatJson) {
        if (!resultNode.isEmpty()) {
            LogUtils.logBusiness("心跳中有消息回执");
            Task lastSentTask = taskManager.getLastSentTask(deviceKey);
            if (lastSentTask != null) {
                taskManager.markTaskSuccess(lastSentTask.getTaskId());
                LogUtils.logBusiness("Result for task {} (device {}): Heartbeat: {}", lastSentTask.getTaskId(), deviceKey, heartbeatJson);
            } else {
                LogUtils.logBusiness("Received task result for device {}, but no last sent task found in Redis.", deviceKey);
            }
        } else {
            LogUtils.logBusiness("心跳中没有消息回执");
        }
        Map<String, String> data = new HashMap<>();
        data.put("/karface/cp/yf/heart.admin", heartbeatJson);
        LogUtils.logBusiness("HTTP 心跳构建的遥测数据为{}", data);
//        DeviceData msg = new DeviceData(deviceKey, data, ProtocolIdentifier.PROTOCOL_VIDEO);
//        producer.onData(msg, DeviceDataEvent.Type.TO_TB);
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

    /**
     * 解析URI中的查询参数
     *
     * @param uri 要解析的URI字符串
     * @return 包含查询参数的键值对映射，若解析失败返回空映射
     */
    private Map<String, String> parseUrlParams(String uri) {
        try {
            // 使用 Netty 框架中的 QueryStringDecoder 来解析 URI 中的查询参数
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            Map<String, String> params = new HashMap<>();
            // 遍历 decoder 解析出的所有参数
            decoder.parameters().forEach((key, value) -> {
                // 对每个键值对进行处理
                // 注意：这里假设每个参数名只对应一个值
                // 如果参数有多个值（如 key=value1&key=value2），则只取第一个值
                params.put(key, value.get(0));
            });
            LogUtils.logBusiness("解析的结果为{}", params);
            return params;
        } catch (Exception e) {
            LogUtils.logError("Error parsing URL parameters", e);
            return Collections.emptyMap(); // Return empty map on error
        }
    }


}
