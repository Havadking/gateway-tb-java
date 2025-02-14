package mqtt;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.GatewayConfig;
import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import lombok.extern.slf4j.Slf4j;
import model.DeviceData;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: gateway-netty
 * @description: MQTT 接收器
 * @author: Havad
 * @create: 2025-02-08 16:22
 **/


@Slf4j
public class MqttReceiver implements MqttCallback {
    /**
     * 设备数据事件生产者
     */
    private final DeviceDataEventProducer producer;
    /**
     * MQTT客户端实例。
     */
    private final MqttClient mqttClient;

    /**
     * 定时任务执行器，使用单线程的ScheduledExecutorService实现。
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * 当前重连次数
     */
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    /**
     * 是否正在重连
     */
    private volatile boolean isReconnecting = false;

    public MqttReceiver(DeviceDataEventProducer producer, MqttClient mqttClient) {
        this.producer = producer;
        this.mqttClient = mqttClient;
        this.mqttClient.setCallback(this); // 设置回调
    }

    public void start() throws MqttException {
        // 订阅服务器的RPC命令
        mqttClient.subscribe(GatewayConfig.RPC_TOPIC);
        log.info("MQTT 订阅 RPC 地址成功");
    }


    /**
     * This method is called when the connection to the server is lost.
     *
     * @param cause the reason behind the loss of connection.
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT connection lost: {}", cause.getMessage());

        // 避免重复重连
        if (isReconnecting) {
            return;
        }
        isReconnecting = true;
        // 重置重连计数器
        reconnectAttempts.set(0);
        scheduleReconnect();
    }

    /**
     * 定时重新连接MQTT代理。
     * <p>
     * 本方法通过指数退避算法计算延时，并在指定时间后尝试重新连接MQTT代理。
     * 若重连尝试次数未超过最大限制，将继续尝试连接；否则，将停止重连尝试并记录错误日志。
     *
     * @see GatewayConfig#RECEIVER_RECONNECT_RETRY
     */
    private void scheduleReconnect() {
        if (reconnectAttempts.get() < GatewayConfig.RECEIVER_RECONNECT_RETRY) {
            // 指数退避
            long delay = (long) Math.pow(2, reconnectAttempts.getAndIncrement());
            log.info("Attempting to reconnect to MQTT broker in {} seconds...", delay);

            scheduler.schedule(() -> {
                try {
                    if (!mqttClient.isConnected()) {
                        mqttClient.reconnect();
                        log.info("Successfully reconnected to MQTT broker.");
                        // 重连成功，重置标志位
                        isReconnecting = false;
                        //重置计数器
                        reconnectAttempts.set(0);
                        //重新订阅
                        start();
                    }
                } catch (Exception e) {
                    log.info("Failed to reconnect to MQTT broker: {}, retry is {}", e.getMessage(), reconnectAttempts.get());
                    // 继续尝试重连
                    scheduleReconnect();
                }
            }, delay, TimeUnit.SECONDS);
        } else {
            log.error("【严重】MQTT订阅失败，已达到最大重试次数【严重】");
            // 达到最大重连次数，停止重连
            isReconnecting = false;
            // todo 是否需要发送短信提醒
        }
    }


    /**
     * 当消息到达时调用的方法
     *
     * @param topic   消息主题
     * @param message MQTT消息内容
     * @throws Exception 当处理消息过程中发生错误时抛出异常
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        log.info("收到来自thingsboard的消息 {}", message);
        String messageContent = new String(message.getPayload());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(messageContent);

        // 获取各个字段值
        String device = rootNode.get("device").asText();
        int id = rootNode.get("data").get("id").asInt();
        // 构造成设备接收所需要的类型
        String body = appendHexLength("*#F#" + rootNode.get("data").get("params").get("body").asText());
        DeviceData data = new DeviceData(device, body, "NORMAL");

        // 放入 Disruptor
        producer.onData(data, DeviceDataEvent.Type.TO_DEVICE);

        // 发送确认收到的信息
        sendConfirmationResponse(topic, device, id);
    }

    /**
     * Called when delivery for a message has been completed, and all
     * acknowledgments have been received. For QoS 0 messages it is
     * called once the message has been handed to the network for
     * delivery. For QoS 1 it is called when PUBACK is received and
     * for QoS 2 when PUBCOMP is received. The token will be the same
     * token as that returned when the message was published.
     *
     * @param token the delivery token associated with the message.
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    /**
     * 将输入字符串的长度转换为4位十六进制表示，并将其追加到字符串末尾。
     *
     * @param input 原始字符串
     * @return 追加了长度十六进制表示的字符串
     */
    public static String appendHexLength(String input) {
        // 获取字符串长度
        int length = input.length();

        // 转换为4位十六进制，不足位数补0
        String hexLength = String.format("%04X", length);

        // 将十六进制长度添加到原字符串末尾
        return input + hexLength;
    }

    /**
     * 发送确认响应到指定Topic。
     *
     * @param originalTopic 原始Topic
     * @param deviceId      设备ID
     * @param requestId     请求ID
     * @throws Exception 发送过程中遇到异常时抛出
     */
    private void sendConfirmationResponse(String originalTopic, String deviceId, int requestId) throws Exception {
        // 构建响应 JSON
        JSONObject responseJson = new JSONObject();
        responseJson.put("device", deviceId);
        responseJson.put("id", requestId);
        JSONObject dataJson = new JSONObject();
        dataJson.put("success", true);
        responseJson.put("data", dataJson);

        // 创建 MQTT 消息
        MqttMessage responseMessage = new MqttMessage(responseJson.toString().getBytes());
        responseMessage.setQos(1); // 设置 QoS 等级

        // 发布响应消息到相同的 Topic
        mqttClient.publish(originalTopic, responseMessage);
    }

}
