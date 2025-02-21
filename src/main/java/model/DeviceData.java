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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 设备ID
     */
    private final String deviceId;
    /**
     * 消息内容
     */
    private final Object msg;

    private final ProtocolIdentifier protocolType; //协议类型

    // 序列化方法
    public byte[] serializeMsg() throws Exception {
        if (msg instanceof String) {
            return ((String) msg).getBytes(CharsetUtil.UTF_8);
        } else {
            // 将对象转换为JSON字符串，再转换为字节数组
            return objectMapper.writeValueAsString(msg).getBytes(CharsetUtil.UTF_8);
        }
    }
}
