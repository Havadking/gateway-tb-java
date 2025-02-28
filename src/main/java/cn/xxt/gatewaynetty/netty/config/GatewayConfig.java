package cn.xxt.gatewaynetty.netty.config;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 网关相关配置
 * @author: Havad
 * @create: 2025-02-07 15:22
 **/

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class GatewayConfig {
    /**
     * 接收来自ThingsBoard的RPC信息的主题
     */
    public static final String RPC_TOPIC = "v1/gateway/rpc"; // 接收来自ThingsBoard的RPC信息
    /**
     * 遥测数据发送的主题名称
     */
    public static final String TELEMETRY_TOPIC = "v1/gateway/telemetry"; // 发送遥测
    /**
     * 设备断开连接的主题标识。
     */
    public static final String DISCONNECT_TOPIC = "v1/gateway/disconnect"; // 声明设备断开链接
    /**
     * 设备连接主题
     */
    public static final String CONNECT_TOPIC = "v1/gateway/connect"; // 声明设备链接
    /**
     * 属性更新发布到服务器的主题
     */
    public static final String ATTRIBUTE_TOPIC = "v1/gateway/attributes"; // 将属性更新发布到服务器

    /**
     * 读数据超时时间，单位为秒，超时后自动断开连接
     */
    public static final int READ_TIME_OUT = 120; // 读数据超时时间,单位为秒， 超时以后自动断开链接

    /**
     * MQTT连接重试最大次数
     */
    public static final int RECEIVER_RECONNECT_RETRY = 10; // MQTT连接重试最大次数
}
