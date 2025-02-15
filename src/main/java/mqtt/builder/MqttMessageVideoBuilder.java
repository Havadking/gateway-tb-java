package mqtt.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import model.DeviceData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import util.LogUtils;
import util.VideoParserUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 视频话机协议的数据构建器
 * @author: Havad
 * @create: 2025-02-14 15:23
 **/

public class MqttMessageVideoBuilder implements MqttMessageBuilder{

    @Override
    public MqttMessage buildMessage(DeviceData deviceData) throws Exception {
        // 获取常量
        String deviceNo = deviceData.getDeviceId();
        @SuppressWarnings("unchecked")
        Map<String, Object> messageMap = (Map<String, Object>) deviceData.getMsg();
        // 获取 command
        String command = (String) messageMap.get("command");
        // 获取 request 内容
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) messageMap.get(VideoParserUtil.getToTBMessageType(command));

        // 创建 JSON 对象
        // 创建嵌套的数据结构
        Map<String, Object> values = new HashMap<>();
        values.put(command, request);  // 使用 command 作为 key，request 作为 value

        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("ts", System.currentTimeMillis());  // 使用给定的时间戳
        dataPoint.put("values", values);

        // 创建最终的 JSON 结构
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put(deviceNo, Collections.singletonList(dataPoint));

        // 转换为 JSON 字符串
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(result);

        // 创建 MQTT 消息对象
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);

        LogUtils.logBusiness("视频话机创建MQTT信息成功{}", message);

        return message;
    }
}
