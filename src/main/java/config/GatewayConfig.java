package config;

/**
 * @program: gateway-netty
 * @description: 网关相关配置
 * @author: Havad
 * @create: 2025-02-07 15:22
 **/

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class GatewayConfig {
    /**
     * 网关对外提供的端口号
     */
    public static final int PORT = 5566; // 网关对外提供的端口号
    /**
     * 视频话机进行人脸传输所使用的HTTP端口
     */
    public static final int HTTP_PORT = 12000; // 用于视频话机进行人脸传输
    /**
     * 视频话机进行文件下发的端口号
     */
    public static final int FILE_PORT = 12001; // 用于视频话机进行文件下发

    /**
     * MQTT代理服务器URL地址。
     * 用于连接到指定thingsboard服务的MQTT协议端点。
     */
    public static final String MQTT_BROKER_URL = "tcp://192.168.9.230:1883"; // 正式thingsboard服务地址
    /**
     * MQTT客户端ID。
     * 用于标识连接到MQTT服务器的客户端。
     */
    public static final String MQTT_CLIENT_ID = "TestClient"; // 可以是随机，不重要，不影响
    /**
     * MQTT协议连接时使用的用户名，对应TB上设置的网关设备凭证。
     */
    public static final String MQTT_USERNAME = "test123"; // TB上的网关对应的设备凭证
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
     * Disruptor 缓冲区大小常量，默认为1024，根据实际情况调整。
     */
    public static final int DISRUPTOR_BUFFER_SIZE = 1024; // Disruptor 缓冲区大小，根据实际情况调整

    /**
     * 读数据超时时间，单位为秒，超时后自动断开连接
     */
    public static final int READ_TIME_OUT = 120; // 读数据超时时间,单位为秒， 超时以后自动断开链接
    /**
     * MQTT遥测发布失败重试次数
     */
    public static final int MQTT_SEND_RETRY = 3; // MQTT遥测发布失败重试次数
    /**
     * MQTT连接重试最大次数
     */
    public static final int RECEIVER_RECONNECT_RETRY = 10; // MQTT连接重试最大次数

}
