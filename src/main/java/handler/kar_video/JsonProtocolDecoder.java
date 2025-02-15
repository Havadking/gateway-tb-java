package handler.kar_video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import util.LogUtils;
import util.PackageAssembler;

import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: gateway-netty
 * @description: 适用于卡尔视频话机的解码器
 * @author: Havad
 * @create: 2025-02-13 16:06
 **/

public class JsonProtocolDecoder extends ByteToMessageDecoder {
    private static final int HEADER_LENGTH = 20; // 根据实际头部长度调整
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final byte[] SYNC_PREFIX = new byte[]{0x40}; // @ 字符
    private static final int MAX_PACKAGE_SIZE = 2000;
    private static final short HEARTBEAT_TYPE = 0x0313;

    // 存储未完成的多包消息
    private final Map<Integer, PackageAssembler> messageAssemblers = new ConcurrentHashMap<>();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HEADER_LENGTH) {
            return;
        }

        in.markReaderIndex();

        // 验证同步标识(2字节)
        byte firstByte = in.readByte();
        byte terminalType = in.readByte();
        if (firstByte != SYNC_PREFIX[0] || !isValidTerminalType(terminalType)) {
            in.resetReaderIndex();
            throw new ProtocolException("无效的同步标识");
        }

        // 读取协议头部
        short protocolType = in.readShort();       // 协议类型(2字节)
        int sessionNumber = in.readInt();          // 会话序号(4字节)
        short totalPackages = in.readShort();      // 协议总包数(2字节)
        short currentPackage = in.readShort();     // 协议包序号(2字节)
        int keyNumber = in.readInt();              // 密钥序号(4字节)
        short encryptionType = in.readShort();     // 加密类型(2字节)
        short dataLength = in.readShort();         // 数据长度(2字节)

        // 验证是否有足够的字节用于完整消息
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        // 读取加密的数据内容
        byte[] encryptedData = new byte[dataLength];
        in.readBytes(encryptedData);

        // 处理多包消息
        if (totalPackages > 1) {
            PackageAssembler assembler = messageAssemblers.computeIfAbsent(sessionNumber,
                    k -> new PackageAssembler(totalPackages));

            assembler.addPackage(currentPackage, encryptedData);

            if (!assembler.isComplete()) {
                return;
            }

            // 获取完整消息并移除组装器
            encryptedData = assembler.getCompleteMessage();
            messageAssemblers.remove(sessionNumber);
        }

        // 解密数据 （不需要）

        // 解析JSON
        String jsonStr = new String(encryptedData, StandardCharsets.UTF_8);
        JsonNode rootNode = objectMapper.readTree(jsonStr);

        // 处理解析后的数据
        Map<String, Object> resultMap = processJsonData(rootNode, protocolType);
        if (resultMap != null) {
            out.add(resultMap);
        }
    }

    /**
     * 创建心跳响应的私有方法
     * <p>
     * 该方法用于构建返回给客户端的心跳响应数据包。
     *
     * @return 包含心跳命令及请求详情的映射对象
     */
    private Map<String, Object> createHeartbeatResponse() {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("command", "heartbeat");
        resultMap.put("request", Collections.singletonMap("heartbeat", "gateway make"));
        LogUtils.logBusiness("构建心跳包: {}", resultMap);
        return resultMap;
    }

    /**
     * 处理JSON数据方法
     *
     * @param rootNode     JSON根节点
     * @param protocolType 协议类型
     * @return 处理后的JSON数据映射，或心跳响应映射，如果发生错误返回null
     */
    private Map<String, Object> processJsonData(JsonNode rootNode, short protocolType) {
        if (protocolType == HEARTBEAT_TYPE || rootNode == null || rootNode.isEmpty()) {
            // 处理心跳包
            return createHeartbeatResponse();
        }

        Map<String, Object> resultMap = new HashMap<>();

        if (rootNode.has("command")) {
            String command = rootNode.get("command").asText();
            resultMap.put("command", command);

            if ("devstatus".equals(command)) {
                LogUtils.logBusiness("设备状态作为属性发送");
                // 处理设备状态 todo
                return null;
            }

            // 处理请求或响应
            if (rootNode.has("request")) {
                resultMap.put("request", objectMapper.convertValue(rootNode.get("request"), Map.class));
            } else if (rootNode.has("response")) {
                resultMap.put("response", objectMapper.convertValue(rootNode.get("response"), Map.class));
            } else {
                LogUtils.logError("数据解析错误: {}", new Throwable(), rootNode);
                return null;
            }
        }
        return resultMap;
    }

    /**
     * 验证终端类型是否有效。
     *
     * @param terminalType 终端类型标识
     * @return 如果终端类型在指定范围内返回true，否则返回false
     */
    private boolean isValidTerminalType(byte terminalType) {
        // 验证终端类型是否有效
        char type = (char) terminalType;
        return type >= 'A' && type <= 'J';
    }
}
