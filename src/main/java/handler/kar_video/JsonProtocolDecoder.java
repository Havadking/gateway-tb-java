package handler.kar_video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.LogUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 适用于卡尔视频话机的解码器
 * @author: Havad
 * @create: 2025-02-13 16:06
 **/

public class JsonProtocolDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 20; // 根据实际头部长度调整
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HEADER_LENGTH) {
            return;
        }

        in.markReaderIndex();
        in.skipBytes(HEADER_LENGTH);

        byte[] jsonBytes = new byte[in.readableBytes()];
        in.readBytes(jsonBytes);

        // 解析 JSON 字符串
        JsonNode rootNode = objectMapper.readTree(new String(jsonBytes));
        LogUtils.logBusiness("rootNode is {}", rootNode);
        // 创建结果 Map
        Map<String, Object> resultMap = new HashMap<>();

        if (rootNode == null || rootNode.isEmpty()) {
            // 在这里进行心跳处理，根据协议，心跳发送空的数据包
            resultMap.put("command", "heartbeat");
            Map<String, String> params = new HashMap<>();
            params.put("heartbeat", "gateway make");
            resultMap.put("request", objectMapper.convertValue(params, Map.class));
            LogUtils.logBusiness("【心跳】构建的心跳包为{}", resultMap);
            out.add(resultMap);
        } else {
            // 其他的方法处理
            // 提取 command 字段
            if (rootNode.has("command")) {
                resultMap.put("command", rootNode.get("command").asText());
            }

            if (resultMap.get("command").equals("devstatus")) {
                // 设备状态，当作属性来发送，不走遥测发送的通道了
                LogUtils.logBusiness("devstatus 作为属性发送");
                // todo 属性发送
            } else {
                // 提取 request 或者 response 对象
                if (rootNode.has("request")) {
                    resultMap.put("request", objectMapper.convertValue(rootNode.get("request"), Map.class));
                } else if (rootNode.has("response")) {
                    resultMap.put("response", objectMapper.convertValue(rootNode.get("response"), Map.class));
                } else {
                    LogUtils.logError("卡尔视频话机解释数据错误:{}", new Throwable(), rootNode);
                    return;
                }
                out.add(resultMap);
            }
        }
    }
}
