package mqtt.builder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import model.DeviceData;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import util.LogUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 用来传http心跳
 * @author: Havad
 * @create: 2025-02-20 15:30
 **/

public class MqttMessageVideoFaceBuilder implements MqttMessageBuilder{
    @Override
    public MqttMessage buildMessage(DeviceData deviceData) throws Exception {
        // 创建 JSON 对象
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode msg = objectMapper.createObjectNode();
        ArrayNode nameArray = objectMapper.createArrayNode();
        ObjectNode nameObject = objectMapper.createObjectNode();

        // 获取相关参数
        String jsonString = (String) deviceData.getMsg();
        String deviceNo = deviceData.getDeviceId();

        // Parse the JSON string into a Map
        Map<String, Object> dataMap = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});

        // Convert the Map to a JsonNode
        JsonNode dataNode = objectMapper.valueToTree(dataMap);

        // Add to the message structure
        nameObject.set("/karface/cp/yf/heart.admin", dataNode);

        nameArray.add(nameObject);
        msg.set(deviceNo, nameArray);

        String payload = objectMapper.writeValueAsString(msg);

        // 创建 MQTT 消息对象
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        LogUtils.logBusiness("视频话机 HTTP 创建 MQTT 信息成功{}", message);

        return message;
    }
}
