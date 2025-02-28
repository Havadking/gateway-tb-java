package cn.xxt.gatewaynetty.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xxt.gatewaynetty.netty.config.GatewayConfig;
import cn.xxt.gatewaynetty.util.LogUtils;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 用于发送给TB，增加了消息队列和限流机制
 * @author: Havad
 * @create: 2025-02-25 14:36
 **/


@Component
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
     * 消息队列最大容量
     */
    private static final int QUEUE_CAPACITY = 1000;

    /**
     * 发布消息的最大速率 (条/秒)
     */
    private static final int MAX_PUBLISH_RATE = 50;

    /**
     * 消息处理队列
     */
    private final BlockingQueue<MqttPublishTask> messageQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    /**
     * 调度器，用于定期从队列提取消息并发布
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 队列处理器是否已启动
     */
    private final AtomicBoolean processorStarted = new AtomicBoolean(false);

    /**
     * 构造函数
     */
    @Autowired
    public MqttSender(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    /**
     * 消息发布任务类
     */
    @Getter
    private static class MqttPublishTask {
        /**
         * 主题
         */
        private final String topic;
        /**
         * MQTT消息对象。
         */
        private final MqttMessage message;

        MqttPublishTask(String topic, MqttMessage message) {
            this.topic = topic;
            this.message = message;
        }

    }

    /**
     * 初始化消息处理器
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public void init() {
        if (processorStarted.compareAndSet(false, true)) {
            // 计算发布间隔（毫秒）
            long publishIntervalMs = 1000 / MAX_PUBLISH_RATE;

            // 启动定时任务，定期从队列中取出消息并发布
            scheduler.scheduleAtFixedRate(this::processQueuedMessages, 0, publishIntervalMs, TimeUnit.MILLISECONDS);

            LogUtils.logBusiness("MQTT消息队列处理器已启动，发布速率限制为：{} 条/秒", MAX_PUBLISH_RATE);
        }
    }

    /**
     * 处理队列中的消息
     */
    @SuppressWarnings("checkstyle:ReturnCount")
    private void processQueuedMessages() {
        if (messageQueue.isEmpty()) {
            return;
        }

        MqttPublishTask task = messageQueue.poll();
        if (task != null) {
            try {
                if (!mqttClient.isConnected()) {
                    try {
                        mqttClient.reconnect();
                    } catch (MqttException e) {
                        LogUtils.logBusiness("MQTT客户端重连失败: {}", e.getMessage());
                        // 重新放回队列稍后处理
                        messageQueue.offer(task);
                        return;
                    }
                }

                // 发布消息
                mqttClient.publish(task.getTopic(), task.getMessage());
                LogUtils.logBusiness("成功发布MQTT消息到主题: {}", task.getTopic());
            } catch (MqttException e) {
                LogUtils.logBusiness("发布MQTT消息失败: {}, 将重新放入队列", e.getMessage());
                // 如果发送失败，重新放入队列尾部稍后再试
//                boolean offered = messageQueue.offer(task);
                LogUtils.logBusiness("当前队列中还有{}条数据", getPendingMessageCount());
//                if (!offered) {
//                    LogUtils.logBusiness("消息队列已满，无法重新入队: {}", task.getTopic());
//                }
            }
        }
    }

    /**
     * 将消息添加到发布队列
     *
     * @param topic   MQTT主题
     * @param message MQTT消息
     * @return 是否成功添加到队列
     */
    private boolean enqueueMessage(String topic, MqttMessage message) {
        // 确保处理器已启动
        if (!processorStarted.get()) {
            init();
        }

        MqttPublishTask task = new MqttPublishTask(topic, message);
        boolean success = messageQueue.offer(task);

        if (!success) {
            LogUtils.logBusiness("MQTT消息队列已满，无法添加新消息: {}", topic);
        }

        return success;
    }

    /**
     * 关闭清理资源
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 发送设备已连接的通知
     *
     * @param deviceNo 设备编号
     * @throws RuntimeException 当生成JSON消息失败时抛出运行时异常
     */
    public void sendDeviceConnected(String deviceNo) {
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

            // 添加到发布队列而不是直接发布
            boolean enqueued = enqueueMessage(GatewayConfig.CONNECT_TOPIC, message);
            if (enqueued) {
                LogUtils.logBusiness("设备连接消息已加入队列: {}", deviceNo);
            } else {
                throw new RuntimeException("设备连接消息加入队列失败: " + deviceNo);
            }
        } catch (JsonProcessingException e) {
            LogUtils.logError("生成设备连接 JSON 消息失败：{}", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送设备断开连接通知
     *
     * @param deviceNo 设备编号
     * @throws RuntimeException 当生成JSON消息失败时抛出运行时异常
     */
    public void sendDeviceDisconnected(String deviceNo) {
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

            // 添加到发布队列而不是直接发布
            boolean enqueued = enqueueMessage(GatewayConfig.DISCONNECT_TOPIC, message);
            if (enqueued) {
                LogUtils.logBusiness("设备断开消息已加入队列: {}", deviceNo);
            } else {
                throw new RuntimeException("设备断开消息加入队列失败: " + deviceNo);
            }
        } catch (JsonProcessingException e) {
            LogUtils.logError("生成设备断开 JSON 消息失败：{}", e);
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
        boolean enqueued = enqueueMessage(GatewayConfig.ATTRIBUTE_TOPIC, message);
        if (enqueued) {
            LogUtils.logBusiness("设备属性消息已加入队列: {}", message);
        } else {
            LogUtils.logBusiness("设备属性消息加入队列失败: {}", message);
            throw new RuntimeException("设备属性消息加入队列失败");
        }
    }

    /**
     * 发送遥测数据到Thingsboard
     *
     * @param message MQTT消息对象，包含设备遥测数据
     * @throws RuntimeException 当通过MQTT发送消息遇到问题时抛出
     */
    public void sendToThingsboard(MqttMessage message) {
        boolean enqueued = enqueueMessage(GatewayConfig.TELEMETRY_TOPIC, message);
        if (enqueued) {
            LogUtils.logBusiness("遥测数据消息已加入队列: {}", message);
        } else {
            LogUtils.logError("遥测数据消息加入队列失败: {}", new Throwable(), message);
            throw new RuntimeException("遥测数据消息加入队列失败");
        }
    }

    /**
     * 当前队列中待处理的消息数量
     *
     * @return 队列中的消息数量
     */
    public int getPendingMessageCount() {
        return messageQueue.size();
    }
}
