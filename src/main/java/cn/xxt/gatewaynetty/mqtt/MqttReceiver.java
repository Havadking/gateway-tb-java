package cn.xxt.gatewaynetty.mqtt;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xxt.gatewaynetty.netty.config.GatewayConfig;
import cn.xxt.gatewaynetty.netty.model.DeviceDataEvent;
import cn.xxt.gatewaynetty.netty.model.DeviceData;
import cn.xxt.gatewaynetty.mqtt.parser.MqttMessageParser;
import cn.xxt.gatewaynetty.mqtt.parser.MqttMessageParserFactory;
import cn.xxt.gatewaynetty.netty.protocol.ProtocolIdentifier;
import cn.xxt.gatewaynetty.netty.videophone.task.Task;
import cn.xxt.gatewaynetty.netty.videophone.task.TaskManager;
import cn.xxt.gatewaynetty.util.LogUtils;
import cn.xxt.gatewaynetty.kafka.DeviceDataKafkaProducer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.xxt.gatewaynetty.util.VideoParserUtil.imageUrlToBase64;


/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: MQTT 接收器
 * @author: Havad
 * @create: 2025-02-08 16:22
 **/


public class MqttReceiver implements MqttCallback {
    /**
     * 设备数据事件生产者
     */
    private final DeviceDataKafkaProducer producer;
    /**
     * MQTT客户端实例。
     */
    private final MqttClient mqttClient;


    /**
     * 任务管理器
     */
    private final TaskManager taskManager;

    /**
     * 定时任务执行器，使用单线程的ScheduledExecutorService实现。
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * 当前重连次数
     */
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    /**
     * MQTT消息解析工厂实例。
     */
    private final MqttMessageParserFactory parserFactory;
    /**
     * 是否正在重连
     */
    private volatile boolean isReconnecting = false;

    public MqttReceiver(DeviceDataKafkaProducer producer,
                        MqttClient mqttClient, TaskManager taskManager,
                        MqttMessageParserFactory parserFactory) {
        this.producer = producer;
        this.mqttClient = mqttClient;
        this.taskManager = taskManager;
        this.mqttClient.setCallback(this); // 设置回调
        this.parserFactory = parserFactory;
    }

    /**
     * 启动方法，用于开始订阅MQTT命令
     *
     * @throws MqttException 当订阅过程中出现异常时抛出
     */
    public void start() throws MqttException {
        // 订阅服务器的RPC命令
        mqttClient.subscribe(GatewayConfig.RPC_TOPIC);
        LogUtils.logBusiness("MQTT 订阅 RPC 地址成功");
    }


    /**
     * 当与服务器的连接丢失时调用此方法。
     *
     * @param cause 连接丢失的原因。
     */
    @Override
    public void connectionLost(Throwable cause) {
        LogUtils.logError("MQTT connection lost: {}", cause, cause.getMessage());

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
            LogUtils.logBusiness("Attempting to reconnect to MQTT broker in {} seconds...", delay);

            scheduler.schedule(() -> {
                try {
                    if (!mqttClient.isConnected()) {
                        mqttClient.reconnect();
                        LogUtils.logBusiness("Successfully reconnected to MQTT broker.");
                        // 重连成功，重置标志位
                        isReconnecting = false;
                        //重置计数器
                        reconnectAttempts.set(0);
                        //重新订阅
                        start();
                    }
                } catch (Exception e) {
                    LogUtils.logBusiness("Failed to reconnect to MQTT broker: {}, retry is {}",
                            e.getMessage(), reconnectAttempts.get());
                    // 继续尝试重连
                    scheduleReconnect();
                }
            }, delay, TimeUnit.SECONDS);
        } else {
            LogUtils.logError("【严重】MQTT订阅失败，已达到最大重试次数【严重】{}", new MqttException(new Exception("订阅失败")));
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
    @SuppressWarnings("checkstyle:ReturnCount")
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        LogUtils.logBusiness("【收到thingsboard】的消息 {}", message);
        String messageContent = new String(message.getPayload());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(messageContent);
        String device = rootNode.get("device").asText();
        JsonNode dataNode = rootNode.get("data");
        String method = rootNode.get("data").get("method").asText();
        int id = rootNode.get("data").get("id").asInt();
        // 1. 根据 Topic 或消息内容确定协议类型
        ProtocolIdentifier protocolType = determineProtocolType(method);
        if (protocolType == null) {
            // 不处理
            LogUtils.logBusiness("【{}】种类信息暂时不处理", method);
            return;
        } else if (protocolType.equals(ProtocolIdentifier.PROTOCOL_VIDEO_FACE)) {
            LogUtils.logBusiness("视频话机白名单处理");
            JsonNode paramsNode = dataNode.get("params");
            String taskId = paramsNode.get("taskId").asText();
            JsonNode bodyNode = paramsNode.get("body");
            List<Map<String, Object>> personList = new ArrayList<>();
            if (bodyNode.isArray()) {
                for (JsonNode personNode : bodyNode) {
                    Map<String, Object> personMap = new HashMap<>();
                    int operType = personNode.get("operType").asInt();
                    if (operType == 1) {
                        LogUtils.logBusiness("下发人脸");
                        personMap.put("operType", operType);
                        personMap.put("id", personNode.get("id").asText());
                        personMap.put("number", personNode.get("number").asText());
                        personMap.put("depName", personNode.get("depName").asText());
                        personMap.put("userType", personNode.get("userType").asInt());
                        personMap.put("name", personNode.get("name").asText());
                        personMap.put("cardNo", personNode.get("cardNo").asText());
                        String base64 = imageUrlToBase64(personNode.get("imageUrl").asText());
                        personMap.put("picture", base64);
                    } else {
                        LogUtils.logBusiness("删除人脸");
                        personMap.put("operType", operType);
                        personMap.put("id", personNode.get("id").asText());
                    }
                    personList.add(personMap);
                }
            }
            Task task = new Task(taskId, device, personList);
            taskManager.addTask(device, task);
            LogUtils.logBusiness("Added task {} for device {}", task.getTaskId(), device);
            return;
        }

        // 2. 获取 MqttMessageParser
        MqttMessageParser parser = parserFactory.getParser(protocolType);

        // 3. 放入 Disruptor
        DeviceData data = parser.parseMessage(message);
        producer.sendData(data, DeviceDataEvent.Type.TO_DEVICE);

        // 4. 发送确认收到的信息
        sendConfirmationResponse(topic, device, id);
    }

    /**
     * 确定协议类型的方法
     *
     * @param method 用于判断协议类型的方法名
     * @return 对应的协议标识符，若没有匹配项则返回null
     */
    @SuppressWarnings("checkstyle:ReturnCount")
    private ProtocolIdentifier determineProtocolType(String method) {
        switch (method) {
            case "send_msg":
                // 普通话机
                LogUtils.logBusiness("普通话机下发数据");
                return ProtocolIdentifier.PROTOCOL_NORMAL;
            case "tcp_rpc":
                // 视频话机基本
                LogUtils.logBusiness("视频话机下发数据");
                return ProtocolIdentifier.PROTOCOL_VIDEO;
            case "add person":
            case "del person":
                // 视频话机人脸
                LogUtils.logBusiness("视频话机人脸操作");
                return ProtocolIdentifier.PROTOCOL_VIDEO_FACE;
            default:
                return null;
        }
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
