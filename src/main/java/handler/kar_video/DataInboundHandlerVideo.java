package handler.kar_video;

import com.fasterxml.jackson.databind.ObjectMapper;
import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import handler.DataInboundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.DeviceData;
import protocol.ProtocolIdentifier;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    //同步标识
    private static final byte asyncIdentityPrefix = 0x40;
    private static final byte asyncIdentitySuffix = 0x47;  // 根据设备类型，如 'G' 对应 0x47
    //协议加密类型
    private static final byte[] encryptionType = {0x27, 0x10};
    private static final byte[] keyIndex = {0x00, 0x00, 0x00, 0x00};
    private static final int MAX_PACKET_SIZE = 2000;
    // 会话序号 (从0开始，每个连接递增, 这里只是演示, 你需要根据每个连接维护一个)
    private static final AtomicInteger sessionCounter = new AtomicInteger(0);
    //协议类型, 需要根据你的实际情况确定
    private static final byte[] protocolType = {0x00, 0x00};

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
        if (request == null) {
            request = (Map<String, Object>) messageMap.get("response");
        }
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

            // 获取递增的 sessionIndex
            int sessionIndex = sessionCounter.getAndIncrement();

            // 2. 构建完整的数据包 (可能需要分包)
            sendData(ctx,protocolType,sessionIndex,jsonResponse);
        } catch (Exception e) {
            log.error("构建认证响应失败", e);
            ctx.close();
        }
    }


    /**
     * 发送数据 (可能需要分包)
     */
    private void sendData(ChannelHandlerContext ctx, byte[] protocolType, int sessionIndex, String jsonData) {
        byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);
        int dataLength = jsonBytes.length;

        // 计算总包数
        int totalPackets = (dataLength + MAX_PACKET_SIZE - 1) / MAX_PACKET_SIZE;

        // 分包发送
        for (int i = 0; i < totalPackets; i++) {
            int start = i * MAX_PACKET_SIZE;
            int end = Math.min((i + 1) * MAX_PACKET_SIZE, dataLength);
            byte[] packetData = new byte[end - start];
            System.arraycopy(jsonBytes, start, packetData, 0, end - start);

            // 构建完整的数据包
            ByteBuf buffer = buildPacket(protocolType, sessionIndex, totalPackets, i, packetData);

            // 发送
            int finalI = i;
            ctx.writeAndFlush(buffer)
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("数据包发送成功 (包序号: {}/{})", finalI + 1, totalPackets);
                        } else {
                            log.error("数据包发送失败 (包序号: {}/{})", finalI + 1, totalPackets, future.cause());
                        }
                    });
        }
    }

    /**
     * 构建一个完整的数据包。
     *
     * @param protocolType 协议类型
     * @param sessionIndex 会话序号
     * @param totalPackets 协议总包数
     * @param packetIndex  协议包序号
     * @param data         数据内容
     * @return 构建好的数据包 ByteBuf 对象
     */
    private ByteBuf buildPacket(byte[] protocolType, int sessionIndex, int totalPackets, int packetIndex, byte[] data) {

        ByteBuf buffer = Unpooled.buffer();

        // 0. 同步标识
        buffer.writeByte(asyncIdentityPrefix);
        buffer.writeByte(asyncIdentitySuffix); // 0x47 (话机，'G')

        // 2. 协议类型
        buffer.writeBytes(protocolType); // {0x00, 0x00} (link)

        // 4. 会话序号
        buffer.writeBytes(intToBytesBigEndian(sessionIndex));

        // 8. 协议总包数
        buffer.writeBytes(shortToBytesBigEndian((short) totalPackets));

        // 10. 协议包序号
        buffer.writeBytes(shortToBytesBigEndian((short) packetIndex));

        // 12. 秘钥序号
        buffer.writeBytes(keyIndex);

        // 16. 协议加密类型
        buffer.writeBytes(encryptionType); // {0x27, 0x10}

        // 18. 加密数据长度
        buffer.writeBytes(shortToBytesBigEndian((short) data.length));

        // 20. 数据内容 (JSON 的 UTF-8 字节)
        buffer.writeBytes(data);

        return buffer;
    }

    /**
     * 将int值转换为字节序列（大端序）
     *
     * @param value 需要转换的int值
     * @return 转换后的字节序列数组
     */
    private byte[] intToBytesBigEndian(int value) {
        return new byte[]{
                (byte) ((value >>> 24) & 0xFF),
                (byte) ((value >>> 16) & 0xFF),
                (byte) ((value >>> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }

    /**
     * 将短整型数据转换为字节数组（大端序）
     *
     * @param value 需要转换的短整型值
     * @return 转换后的字节数组
     */
    private byte[] shortToBytesBigEndian(short value) {
        return new byte[]{
                (byte) ((value >>> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
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
