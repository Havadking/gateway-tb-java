package cn.xxt.gatewaynetty.mqtt.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cn.xxt.gatewaynetty.netty.model.DeviceData;
import cn.xxt.gatewaynetty.util.LogUtils;
import cn.xxt.gatewaynetty.util.PDUUtil;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 普通话机协议的数据构建器
 * @author: Havad
 * @create: 2025-02-14 15:23
 **/

public class MqttMessageNormalBuilder implements MqttMessageBuilder {

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public MqttMessage buildMessage(DeviceData deviceData) throws Exception {
        // 创建 JSON 对象
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode msg = objectMapper.createObjectNode();
        ArrayNode nameArray = objectMapper.createArrayNode();
        ObjectNode nameObject = objectMapper.createObjectNode();

        // 获取相关参数
        String pdu = (String) deviceData.getMsg();
        String deviceNo = PDUUtil.getDeviceNo(pdu);
        String data = pdu.substring(4);

        nameObject.put("INFO", data);
        nameArray.add(nameObject);
        msg.set(deviceNo, nameArray);

        String payload = objectMapper.writeValueAsString(msg);

        // 创建 MQTT 消息对象
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        LogUtils.logBusiness("普通话机创建MQTT信息成功{}", message);

        return message;
    }
}
