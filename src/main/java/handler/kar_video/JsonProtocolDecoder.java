package handler.kar_video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger log = LoggerFactory.getLogger(JsonProtocolDecoder.class);
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
        log.info("rootNode is {}", rootNode);

        // 创建结果 Map
        Map<String, Object> resultMap = new HashMap<>();

        // 提取 command 字段
        if (rootNode.has("command")) {
            resultMap.put("command", rootNode.get("command").asText());
        }

        // 提取 request 或者 response 对象
        if (rootNode.has("request")) {
            resultMap.put("request", objectMapper.convertValue(rootNode.get("request"), Map.class));
        } else if (rootNode.has("response")) {
            resultMap.put("response", objectMapper.convertValue(rootNode.get("response"), Map.class));
        } else {
            log.error("卡尔视频话机解释数据错误:{}", rootNode);
        }

        out.add(resultMap);
    }
}
