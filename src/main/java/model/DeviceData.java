package model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import protocol.ProtocolIdentifier;

/**
 * @program: gateway-netty
 * @description: 设备数据
 * @author: Havad
 * @create: 2025-02-12 10:39
 **/

@AllArgsConstructor
@Data
public class DeviceData {
    /**
     * 对象映射器，用于JSON序列化和反序列化操作。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 设备ID
     */
    private final String deviceId;
    /**
     * 消息内容
     */
    private final Object msg;

    /**
     * 协议类型
     */
    private final ProtocolIdentifier protocolType;

    /**
     * 序列化消息方法
     *
     * @return 消息序列化后的字节数组
     * @throws Exception 当序列化过程中发生错误时抛出
     */
    public byte[] serializeMsg() throws Exception {
        if (msg instanceof String) {
            return ((String) msg).getBytes(CharsetUtil.UTF_8);
        } else {
            // 将对象转换为JSON字符串，再转换为字节数组
            return OBJECT_MAPPER.writeValueAsString(msg).getBytes(CharsetUtil.UTF_8);
        }
    }
}
