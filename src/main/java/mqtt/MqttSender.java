package mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import config.GatewayConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import util.LogUtil;
import util.PDUUtil;

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
@Slf4j
public class MqttSender {
    private final MqttClient mqttClient;

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
            LogUtil.info(this.getClass().getName(), "sendDeviceConnected", deviceNo, "设备连接");
        } catch (JsonProcessingException e) {
            log.error("生成设备连接 JSON 消息失败：{}", e.getMessage());
            throw new RuntimeException(e);
        } catch (MqttException e) {
            log.error("发布设备连接 MQTT 消息失败：{}", e.getMessage());
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
            LogUtil.info(this.getClass().getName(), "sendDeviceDisconnected", deviceNo, "设备断开");
        } catch (JsonProcessingException e) {
            log.error("生成设备断开 JSON 消息失败：{}", e.getMessage());
            throw new RuntimeException(e);
        } catch (MqttException e) {
            log.error("发布设备断开 MQTT 消息失败：{}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    /**
     * 发送设备遥测数据
     * 只适用于卡尔和掇月的普通话机
     * @param PDU 要发送的数据协议数据单元（Protocol Data Unit）
     * @throws RuntimeException 当生成JSON消息或发布MQTT消息失败时抛出运行时异常
     */
    public void sendDeviceTelemetry(String PDU) {
        int maxRetries = 3;  // 最大重试次数
        int baseIntervalMs = 1000;  // 基础重试间隔（毫秒）
        int maxIntervalMs = 10000;  // 最大重试间隔（毫秒）
        int currentRetry = 0;

        while (currentRetry <= maxRetries) {
            try {
                // 获取相关参数
                String deviceNo = PDUUtil.getDeviceNo(PDU);
                String data = PDU.substring(4);

                // 创建 JSON 对象
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode msg = objectMapper.createObjectNode();
                ArrayNode nameArray = objectMapper.createArrayNode();
                ObjectNode nameObject = objectMapper.createObjectNode();
                nameObject.put("INFO", data);
                nameArray.add(nameObject);
                msg.set(deviceNo, nameArray);

                String payload = objectMapper.writeValueAsString(msg);
                log.info("正在发送遥测数据 (第 {} 次尝试): {}", currentRetry + 1, payload);

                // 创建 MQTT 消息对象
                MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                message.setQos(1);

                // 发布消息
                mqttClient.publish(GatewayConfig.TELEMETRY_TOPIC, message);
                LogUtil.info(this.getClass().getName(), "sendDeviceTelemetry", PDU,
                        String.format("发送遥测成功（第 %d 次尝试）", currentRetry + 1));

                // 发送成功，退出循环
                break;

            } catch (Exception e) {
                currentRetry++;

                if (currentRetry > maxRetries) {
                    log.error("发送遥测数据失败，已达到最大重试次数（{}次）：{}", maxRetries, e.getMessage());
                    throw new RuntimeException("发送遥测数据失败，已达到最大重试次数", e);
                }

                // 计算指数退避时间
                int retryIntervalMs = Math.min(
                        baseIntervalMs * (1 << (currentRetry - 1)) + new Random().nextInt(1000),
                        maxIntervalMs
                );

                log.warn("发送遥测数据失败，将在 {} 毫秒后进行第 {} 次重试。错误信息：{}",
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
