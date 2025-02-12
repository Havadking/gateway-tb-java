package model;

import lombok.AllArgsConstructor;
import lombok.Data;

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
     * 设备ID
     */
    private final String deviceId;
    /**
     * 消息内容
     */
    private final String msg;
}
