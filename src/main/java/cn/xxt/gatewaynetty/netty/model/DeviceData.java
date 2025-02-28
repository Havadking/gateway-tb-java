package cn.xxt.gatewaynetty.netty.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xxt.gatewaynetty.netty.protocol.ProtocolIdentifier;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 设备数据
 * @author: Havad
 * @create: 2025-02-12 10:39
 **/


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceData {
    /**
     * 对象映射器，用于JSON序列化和反序列化操作。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 设备ID
     */
    private String deviceId;
    /**
     * 消息内容
     */
    private Object msg;

    /**
     * 协议类型
     */
    private ProtocolIdentifier protocolType;

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
