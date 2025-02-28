package gataway.mqtt.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gataway.model.DeviceData;
import gataway.protocol.ProtocolIdentifier;
import gataway.util.LogUtils;
import gataway.util.VideoParserUtil;
import io.netty.util.CharsetUtil;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 用于卡尔视频话机的数据解析器
 * @author: Havad
 * @create: 2025-02-14 11:18
 **/

public class MqttVideoMessageParser implements MqttMessageParser {

    @Override
    public DeviceData parseMessage(MqttMessage message) throws Exception {
        // 创建 ObjectMapper 用于 JSON 解析
        ObjectMapper mapper = new ObjectMapper();
        // 将消息内容转换为字符串
        String messageContent = new String(message.getPayload(), CharsetUtil.UTF_8);

        // 解析原始 JSON
        JsonNode rootNode = mapper.readTree(messageContent);

        // 获取设备ID
        String deviceId = rootNode.get("device").asText();

        // 获取 data.params 节点
        JsonNode paramsNode = rootNode.get("data").get("params");

        // 构造新地响应格式
        Map<String, Object> responseMsg = new HashMap<>();
        String command = paramsNode.get("command").asText();

        responseMsg.put("type", "terminal");
        responseMsg.put("command", command);
        responseMsg.put(VideoParserUtil.getToDeviceMessageType(command), paramsNode.get("data"));

        LogUtils.logBusiness("视频话机解析的值为:{}", responseMsg);
        // 创建并返回 DeviceData 对象
        return new DeviceData(deviceId, responseMsg, ProtocolIdentifier.PROTOCOL_VIDEO);
    }
}
