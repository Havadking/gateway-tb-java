package mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.GatewayConfig;
import lombok.AllArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import util.LogUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @program: gateway-netty
 * @description: 用于发送给TB
 * @author: Havad
 * @create: 2025-02-08 14:36
 **/

@AllArgsConstructor
public class MqttSender {
    /**
     * MQTT客户端实例。
     */
    private final MqttClient mqttClient;
    /**
     * 随机数生成器实例。
     */
    private static final Random RANDOM = new Random();


    /**
     * 发送设备已连接的通知
     *
     * @param deviceNo 设备编号
     * @throws RuntimeException 当生成JSON消息或发布MQTT消息失败时抛出运行时异常
     */
    public void sendDeviceConnected(String deviceNo) {
        /*
          参考：
          设备连接API
          通知ThingsBoard设备已连接到网关需要发布以下消息：
          Topic: v1/gateway/connect
          Message: {"device":"Device A"}
          Device A 代表你的设备名称。
          收到后ThingsBoard将查找或创建具有指定名称的设备。
          ThingsBoard还将向该网关发布有关特定设备的新属性更新和RPC命令的消息。
         */

        // 构建信息载荷
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("device", deviceNo);

        // 使用 Jackson 将 Map 转换成 JSON 字符串
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String payload = objectMapper.writeValueAsString(payloadMap);

            // 创建MQTT消息对象
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);

            // 发布消息到指定topic
            mqttClient.publish(GatewayConfig.CONNECT_TOPIC, message);
            LogUtils.logBusiness("设备连接{}", deviceNo);
        } catch (JsonProcessingException e) {
            LogUtils.logError("生成设备连接 JSON 消息失败：{}", e);
            throw new RuntimeException(e);
        } catch (MqttException e) {
            LogUtils.logError("发布设备连接 MQTT 消息失败：{}", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * 发送设备断开连接通知
     *
     * @param deviceNo 设备编号
     * @throws RuntimeException 当生成JSON消息失败或发布MQTT消息失败时抛出运行时异常
     */
    public void sendDeviceDisconnected(String deviceNo) {
        /*
          参考：
          设备断开API
          为了通知ThingsBoard设备已与网关断开连接，需要发布以下消息：
          Topic: v1/gateway/disconnect
          Message: {"device":"Device A"}
          Device A 代表你的设备名称。
          一旦收到ThingsBoard将不再将此特定设备的更新发布到此网关。
         */

        // 构建信息载荷
        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("device", deviceNo);

        // 使用 Jackson 将 Map 转换成 JSON 字符串
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String payload = objectMapper.writeValueAsString(payloadMap);

            // 创建MQTT消息对象
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);

            // 发布消息到指定topic
            mqttClient.publish(GatewayConfig.DISCONNECT_TOPIC, message);
            LogUtils.logBusiness("设备断开{}", deviceNo);
        } catch (JsonProcessingException e) {
            LogUtils.logError("生成设备断开 JSON 消息失败：{}", e);
            throw new RuntimeException(e);
        } catch (MqttException e) {
            LogUtils.logError("发布设备断开 MQTT 消息失败：{}", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * 发送Mqtt消息属性。
     *
     * @param message 要发送的Mqtt消息对象
     * @throws RuntimeException 当发送Mqtt消息失败时抛出运行时异常
     */
    public void sendAttribute(MqttMessage message) {
        try {
            // 发布消息到指定topic
            mqttClient.publish(GatewayConfig.ATTRIBUTE_TOPIC, message);
            LogUtils.logBusiness("发送设备属性{}", message);
        } catch (MqttException e) {
            LogUtils.logError("发送设备属性 MQTT 消息失败：{}", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 发送遥测数据到Thingsboard
     *
     * @param message MQTT消息对象，包含设备遥测数据
     * @throws MqttException 当通过MQTT发送消息遇到问题时抛出
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public void sendToThingsboard(MqttMessage message) throws MqttException {
        int maxRetries = GatewayConfig.MQTT_SEND_RETRY;  // 最大重试次数
        int baseIntervalMs = 1000;  // 基础重试间隔（毫秒）
        int maxIntervalMs = 10000;  // 最大重试间隔（毫秒）
        int currentRetry = 0;
        while (currentRetry <= maxRetries) {
            try {
                LogUtils.logBusiness("正在发送遥测数据 (第 {} 次尝试): {}", currentRetry + 1, message);
                // 发布消息
                mqttClient.publish(GatewayConfig.TELEMETRY_TOPIC, message);
                LogUtils.logBusiness("发送遥测数据[{}]成功（第{}次尝试）", message, currentRetry + 1);
                // 发送成功，退出循环
                break;
            } catch (Exception e) {
                if (!mqttClient.isConnected()) {
                    mqttClient.reconnect();
                }
                currentRetry++;
                if (currentRetry > maxRetries) {
                    LogUtils.logError("{}发送遥测数据失败，已达到最大重试次数（{}次）", e, maxRetries);
                    throw new RuntimeException("发送遥测数据失败，已达到最大重试次数", e);
                }
                // 计算指数退避时间
                int retryIntervalMs = Math.min(
                        baseIntervalMs * (1 << (currentRetry - 1)) + RANDOM.nextInt(1000),
                        maxIntervalMs
                );
                LogUtils.logBusiness("发送遥测数据失败，将在 {} 毫秒后进行第 {} 次重试。错误信息：{}",
                        retryIntervalMs, currentRetry, e.getMessage());
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }
    }
}
