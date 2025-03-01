package cn.xxt.gatewaynetty.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 用于处理视频话机协议格式的工具类
 * @author: Havad
 * @create: 2025-02-14 17:26
 **/

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class VideoParserUtil {
    /**
     * 同步标识的前缀字节
     */
    private static final byte ASYNC_IDENTITY_PREFIX = 0x40;
    /**
     * 异步标识后缀，对应设备类型字符 'G' 的十六进制值
     */
    private static final byte ASYNC_IDENTITY_SUFFIX = 0x47;  // 根据设备类型，如 'G' 对应 0x47
    /**
     * 协议加密类型
     */
    private static final byte[] ENCRYPTION_TYPE = {0x27, 0x10};
    /**
     * 密钥索引
     */
    private static final byte[] KEY_INDEX = {0x00, 0x00, 0x00, 0x00};
    /**
     * 最大数据包大小限制。
     */
    private static final int MAX_PACKET_SIZE = 2000;
    /**
     * 会话序号属性键，用于存储从0开始递增的会话计数器
     */// 会话序号 (从0开始，每个连接递增)
    // 定义一个AttributeKey来存储会话计数器
    private static final AttributeKey<AtomicInteger> SESSION_COUNTER =
            AttributeKey.valueOf("SESSION_COUNTER");

    /**
     * 将图片URL转换为Base64编码的字符串
     *
     * @param imageUrl 图片的URL地址
     * @return 图片内容的Base64编码
     * @throws IOException 当读取图片数据或配置SSL时发生错误
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static String imageUrlToBase64(String imageUrl) throws IOException {
        // 如果是https链接，禁用SSL证书验证
        if (imageUrl.toLowerCase().startsWith("https")) {
            try {
                // 创建信任所有证书的信任管理器
                TrustManager[] trustAllCertificates = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };

                // 安装信任管理器
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCertificates, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IOException("配置SSL失败: " + e.getMessage(), e);
            }
        }

        // 读取图片数据
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // 转换为Base64编码
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }

    /**
     * 根据命令获取消息类型
     *
     * @param command 命令字符串
     * @return 对应的消息类型，如果命令为null或不在预定义列表中返回"response"，否则返回"request"
     */
    public static String getToDeviceMessageType(String command) {
        if (command == null) {
            return "response";
        }

        switch (command) {
            case "setConfigInfo":
            case "getConfigInfo":
            case "querySIM":
            case "notice":
                return "request";
            default:
                return "response";
        }
    }

    /**
     * 根据命令获取对应的ToTB消息类型
     *
     * @param command 指令字符串
     * @return 对应的ToTB消息类型，"request"或"response"
     */
    public static String getToTBMessageType(String command) {
        if (command == null) {
            return "request";
        }

        switch (command) {
            case "setConfigInfo":
            case "getConfigInfo":
            case "querySIM":
            case "notice":
                return "response";
            default:
                return "request";
        }
    }

    /**
     * 获取并递增会话计数器
     *
     * @param channel 当前连接的Channel
     * @return 递增后的会话号
     */
    private static int getAndIncrementSessionCounter(Channel channel) {
        AtomicInteger counter = channel.attr(SESSION_COUNTER).get();
        if (counter == null) {
            counter = new AtomicInteger(0);
            channel.attr(SESSION_COUNTER).set(counter);
        }
        return counter.getAndIncrement();
    }

    /**
     * 发送数据方法，支持大数据分包传输。
     *
     * @param channel  通道对象，用于发送数据
     * @param method   请求的方法名
     * @param jsonData 需要发送的JSON格式数据
     */
    public static void sendData(Channel channel, String method, String jsonData) {
        byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);
        int dataLength = jsonBytes.length;
        int sessionIndex = getAndIncrementSessionCounter(channel);
        byte[] protocolType = ProtocolTypeMapper.getProtocolTypeByMethod(method);

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
            channel.writeAndFlush(buffer)
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            LogUtils.logBusiness("数据包发送成功 (包序号: {}/{})", finalI + 1, totalPackets);
                        } else {
                            LogUtils.logError("数据包发送失败 (包序号: {}/{})",
                                    new Throwable(), finalI + 1, totalPackets, future.cause());
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
    private static ByteBuf buildPacket(
            byte[] protocolType, int sessionIndex, int totalPackets, int packetIndex, byte[] data) {

        ByteBuf buffer = Unpooled.buffer();

        // 0. 同步标识
        buffer.writeByte(ASYNC_IDENTITY_PREFIX);
        buffer.writeByte(ASYNC_IDENTITY_SUFFIX); // 0x47 (话机，'G')

        // 2. 协议类型
        buffer.writeBytes(protocolType); // {0x00, 0x00} (link)

        // 4. 会话序号
        buffer.writeBytes(intToBytesBigEndian(sessionIndex));

        // 8. 协议总包数
        buffer.writeBytes(shortToBytesBigEndian((short) totalPackets));

        // 10. 协议包序号
        buffer.writeBytes(shortToBytesBigEndian((short) packetIndex));

        // 12. 秘钥序号
        buffer.writeBytes(KEY_INDEX);

        // 16. 协议加密类型
        buffer.writeBytes(ENCRYPTION_TYPE); // {0x27, 0x10}

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
    @SuppressWarnings("checkstyle:MagicNumber")
    private static byte[] intToBytesBigEndian(int value) {
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
    @SuppressWarnings("checkstyle:MagicNumber")
    private static byte[] shortToBytesBigEndian(short value) {
        return new byte[]{
                (byte) ((value >>> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }
}
