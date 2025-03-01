package cn.xxt.gatewaynetty.netty.model;

import lombok.Data;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: Disruptor 事件
 * @author: Havad
 * @create: 2025-02-08 17:03
 **/

@Data
public class DeviceDataEvent {
    /**
     * 设备数据值
     */
    private DeviceData value;
    /**
     * 类型
     */
    private Type type;

    public enum Type {
        /**
         * 发送给TB
         */
        TO_TB,
        /**
         * 发送给设备的指令标记
         */
        TO_DEVICE //发送给设备
    }

    /**
     * 清空当前对象中的值
     */
    public void clear() {
        value = null;
    }
}
