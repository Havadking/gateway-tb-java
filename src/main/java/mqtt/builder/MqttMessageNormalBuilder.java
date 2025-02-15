package mqtt.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import model.DeviceData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import util.LogUtils;
import util.PDUUtil;

import java.nio.charset.StandardCharsets;

/**
 * @program: gateway-netty
 * @description: 普通话机协议的数据构建器
 * @author: Havad
 * @create: 2025-02-14 15:23
 **/

public class MqttMessageNormalBuilder implements MqttMessageBuilder {

    @Override
    public MqttMessage buildMessage(DeviceData deviceData) throws Exception {
        // 创建 JSON 对象
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode msg = objectMapper.createObjectNode();
        ArrayNode nameArray = objectMapper.createArrayNode();
        ObjectNode nameObject = objectMapper.createObjectNode();

        // 获取相关参数
        String PDU = (String) deviceData.getMsg();
        String deviceNo = PDUUtil.getDeviceNo(PDU);
        String data = PDU.substring(4);

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
