package config;

/**
 * @program: gateway-netty
 * @description: 网关相关配置
 * @author: Havad
 * @create: 2025-02-07 15:22
 **/

public class GatewayConfig {
    public static final int PORT = 5566;
    public static final String MQTT_BROKER_URL = "tcp://192.168.9.230:1883";
    public static final String MQTT_CLIENT_ID = "TestClient"; // 可以是随机，不重要，不影响
    public static final String MQTT_USERNAME = "test123"; // TB上的网关对应的设备凭证
    public static final String MQTT_PASSWORD = ""; // 不需要，为空即可。现在的设计为仅通过设备凭证进行认证
    public static final int DISRUPTOR_BUFFER_SIZE = 1024; // Disruptor 缓冲区大小，根据实际情况调整
    public static final String RPC_TOPIC = "v1/gateway/rpc";
    public static final String TELEMETRY_TOPIC = "v1/gateway/telemetry";
    public static final String DISCONNECT_TOPIC = "v1/gateway/disconnect";
    public static final String CONNECT_TOPIC = "v1/gateway/connect";
}
