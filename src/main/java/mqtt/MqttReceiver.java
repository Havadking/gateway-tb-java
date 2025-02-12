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

    public MqttReceiver(DeviceDataEventProducer producer, MqttClient mqttClient) {
        this.producer = producer;
        this.mqttClient = mqttClient;
        this.mqttClient.setCallback(this); // 设置回调
    }

    public void start() throws MqttException {
        // 订阅服务器的RPC命令
        mqttClient.subscribe(GatewayConfig.RPC_TOPIC);
    }


    /**
     * This method is called when the connection to the server is lost.
     *
     * @param cause the reason behind the loss of connection.
     */
    @Override
    public void connectionLost(Throwable cause) {
        // todo
        // *** 实现：MQTT 连接丢失处理 ***
        // 例如：记录日志、尝试重连等
        log.error("MQTT connection lost: {}", cause.getMessage());
        // 可以在这里实现 MQTT 重连逻辑

    }

    /**
     * This method is called when a message arrives from the server.
     *
     * <p>
     * This method is invoked synchronously by the MQTT client. An
     * acknowledgment is not sent back to the server until this
     * method returns cleanly.</p>
     * <p>
     * If an implementation of this method throws an <code>Exception</code>, then the
     * client will be shut down.  When the client is next re-connected, any QoS
     * 1 or 2 messages will be redelivered by the server.</p>
     * <p>
     * Any additional messages which arrive while an
     * implementation of this method is running, will build up in memory, and
     * will then back up on the network.</p>
     * <p>
     * If an application needs to persist data, then it
     * should ensure the data is persisted prior to returning from this method, as
     * after returning from this method, the message is considered to have been
     * delivered, and will not be reproducible.</p>
     * <p>
     * It is possible to send a new message within an implementation of this callback
     * (for example, a response to this message), but the implementation must not
     * disconnect the client, as it will be impossible to send an acknowledgment for
     * the message being processed, and a deadlock will occur.</p>
     *
     * @param topic   name of the topic on the message was published to
     * @param message the actual message.
     * @throws Exception if a terminal error has occurred, and the client should be
     *                   shut down.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String messageContent = new String(message.getPayload());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(messageContent);

        // 获取各个字段值
        String device = rootNode.get("device").asText();
        int id = rootNode.get("data").get("id").asInt();
        // 构造成设备接收所需要的类型
        String body = appendHexLength("*#F#" + rootNode.get("data").get("params").get("body").asText());
        DeviceData data = new DeviceData(device, body);

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
