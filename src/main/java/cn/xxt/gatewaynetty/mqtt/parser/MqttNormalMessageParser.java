package cn.xxt.gatewaynetty.mqtt.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xxt.gatewaynetty.netty.model.DeviceData;
import cn.xxt.gatewaynetty.netty.protocol.ProtocolIdentifier;
import cn.xxt.gatewaynetty.util.LogUtils;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 用于卡尔、掇月普通话机的数据解析器
 * @author: Havad
 * @create: 2025-02-14 11:17
 **/

public class MqttNormalMessageParser implements MqttMessageParser {

    /**
     * 解析接收到的MQTT消息为设备数据对象
     *
     * @param message MQTT消息对象
     * @return 解析后的设备数据对象
     * @throws Exception 当解析过程发生错误时抛出异常
     */
    @Override
    public DeviceData parseMessage(MqttMessage message) throws Exception {
        String messageContent = new String(message.getPayload());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(messageContent);

        // 获取各个字段值
        String device = rootNode.get("device").asText();
        // 构造成设备接收所需要的类型
        String body = appendHexLength("*#F#" + rootNode.get("data").get("params").get("body").asText());
        LogUtils.logBusiness("普通话机解析的值为:{}", body);
        return new DeviceData(device, body, ProtocolIdentifier.PROTOCOL_NORMAL);
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
}
