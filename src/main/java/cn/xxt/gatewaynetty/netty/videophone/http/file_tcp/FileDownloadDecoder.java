package cn.xxt.gatewaynetty.netty.videophone.http.file_tcp;

import cn.xxt.gatewaynetty.netty.config.RedisConfig;
import cn.xxt.gatewaynetty.util.LogUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import redis.clients.jedis.JedisPooled;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description:
 * @author: Havad
 * @create: 2025-02-21 17:40
 **/

public class FileDownloadDecoder extends ByteToMessageDecoder {

    /**
     * 同步头信息。
     */
    private static final byte SYNC_HEADER = (byte) 0xA5;
    /**
     * 类型常量，表示特定的类型标识。
     */
    private static final byte TYPE = (byte) 0x12;

    /**
     * Redis键前缀，用于图像相关数据的存储。
     */
    private static final String REDIS_KEY_PREFIX = "tb:image:";

    /**
     * 一个兆 字节的大小，以千字节为单位，常量值为1024。
     */
    private static final int ONE_MEGABYTE = 1024;
    /**
     * 文件ID前缀长度。
     */
    private static final int FILE_ID_PREFIX_LENGTH = 128;


    @SuppressWarnings({"checkstyle:Regexp", "checkstyle:MagicNumber"})
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 获取 redis
        JedisPooled jedisPooled = RedisConfig.getJedisPool();

        // 检查是否有足够的字节来读取头部字段
        // 我们至少需要5个字节（同步头、类型、长度、包号、总包数）
        if (in.readableBytes() < 5) {
            return; // 数据不足，等待更多数据
        }

        // 标记当前读取位置以便需要时重置
        in.markReaderIndex();

        // 读取同步头
        byte syncHeader = in.readByte();
        if (syncHeader != SYNC_HEADER) {
            in.resetReaderIndex();
            throw new IllegalStateException("无效的同步头: " + String.format("0x%02X", syncHeader));
        }

        // 读取类型
        byte type = in.readByte();
        if (type != TYPE) {
            in.resetReaderIndex();
            throw new IllegalStateException("无效的类型: " + String.format("0x%02X", type));
        }

        // 读取长度（N）
        int length = in.readShort(); // 转换为无符号

        // 读取包号
        int packageNumber = in.readShort();

        // 读取总包数
        int totalPackages = in.readShort();

        LogUtils.logBusiness("长度{}, 包号{}, 总报数{}", length, packageNumber, totalPackages);

        // 读取文件ID（剩余的所有内容）
        byte[] fileId;
        if (in.readableBytes() > 0) {
            fileId = new byte[in.readableBytes()];
            in.readBytes(fileId);
        } else {
            fileId = new byte[0]; // 如果没有剩余字节，则创建空数组
        }

        // 创建包含所有解析字段的消息对象
        // 解析文件ID
        String fileIdString = new String(fileId, StandardCharsets.UTF_8).trim();
        String fileIdentifier = parseFileIdentifier(fileIdString);
        LogUtils.logBusiness("File id is {}", fileIdentifier);

        // todo 这个 key 应该是上面的 id， 为了测试先写死
//        String key = REDIS_KEY_PREFIX + "7680093057de4bdfa14dc5dcf4642ad6f73f0ddf-adbb-40fc-80e7-3a9ee3c9ef62";
        // 根据文件标识符从 Redis 中获取图像数据
        String key = REDIS_KEY_PREFIX + fileIdentifier;
        // 获取图像数据
        byte[] bytes = jedisPooled.get(key.getBytes());
        // 发送图像响应数据
        sendImageResponse(ctx, fileIdentifier, bytes);
    }

    /**
     * 从文件ID字符串解析出文件标识符。
     *
     * @param fileIdString 文件ID字符串，格式为：[路径]/[类型],[扩展名],[标识符]
     * @return 解析后的文件标识符，如果解析失败则返回原始字符串
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private String parseFileIdentifier(String fileIdString) {
        // 移除所有空字符（ASCII 0x00）
        fileIdString = fileIdString.replace("\u0000", "");

        // 按逗号分割字符串
        String[] parts = fileIdString.split(",");

        // 如果有至少三个部分（路径,类型,标识符），则返回第三部分作为文件标识符
        if (parts.length >= 3) {
            return parts[2];
        }

        // 如果格式不符合预期，则返回原始字符串
        return fileIdString;
    }

    /**
     * 发送图像响应数据到ChannelHandlerContext
     *
     * @param ctx            ChannelHandlerContext上下文
     * @param fileIdentifier 文件标识符
     * @param imageData      图像数据的字节数组
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void sendImageResponse(ChannelHandlerContext ctx, String fileIdentifier, byte[] imageData) {
        // 准备文件ID字节（第一个包的前 128 字节）
        byte[] fileIdBytes = new byte[FILE_ID_PREFIX_LENGTH];
        byte[] sourceBytes = fileIdentifier.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(sourceBytes, 0, fileIdBytes, 0, Math.min(sourceBytes.length, FILE_ID_PREFIX_LENGTH));

        // 构建整个数据包
        byte[] all = new byte[fileIdBytes.length + imageData.length];
        System.arraycopy(fileIdBytes, 0, all, 0, fileIdBytes.length);
        System.arraycopy(imageData, 0, all, fileIdBytes.length, imageData.length);
        LogUtils.logBusiness("id的长{},数据的长{},总长{}", fileIdBytes.length, imageData.length, all.length);
        // 计算包数，并分包发送
        int totalPackets = (int) Math.ceil((double) all.length / ONE_MEGABYTE);
        for (int packetIndex = 0; packetIndex < totalPackets; packetIndex++) {
            int currentPacketDataLength = Math.min(ONE_MEGABYTE, all.length - packetIndex * ONE_MEGABYTE);
            // 同步头+类型+长度+包号+总包数+数据
            int packetTotalLength = 1 + 1 + 2 + 2 + 2 + currentPacketDataLength;
            ByteBuf packet = ctx.alloc().buffer(packetTotalLength);
            // 构建下发数据
            packet.writeByte(0xa5);
            packet.writeByte(0x12);
            // 长度字段只包含其之后的数据长度, 且为short类型
            packet.writeShort(currentPacketDataLength + 4);
            packet.writeShort(packetIndex);
            packet.writeShort(totalPackets);
            packet.writeBytes(all, packetIndex * ONE_MEGABYTE, currentPacketDataLength);
            // 下发数据
            ctx.writeAndFlush(packet);
            LogUtils.logBusiness("发送第 {} 包，共 {} 包，当前包数据长度: {} 字节",
                    packetIndex + 1, totalPackets, currentPacketDataLength);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LogUtils.logError("发送过程中被中断", e);
                break;
            }
        }
        LogUtils.logBusiness("文件下发完成，共发送 {} 包数据", totalPackets);
        ctx.close();
        LogUtils.logBusiness("关闭当前的 TCP 文件连接");
    }
}
