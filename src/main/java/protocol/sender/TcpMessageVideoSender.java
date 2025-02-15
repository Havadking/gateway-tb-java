package protocol.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import model.DeviceData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.LogUtils;
import util.VideoParserUtil;

import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 适用于卡尔视频话机的发送器
 * @author: Havad
 * @create: 2025-02-15 09:39
 **/

public class TcpMessageVideoSender implements TcpMessageSender {
    /**
     * 对象映射器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void sendMessageToDevice(DeviceData data, Channel channel) throws JsonProcessingException {
        LogUtils.logBusiness("【发往设备】【视频话机】数据为:{}", data);
        // 转换为JSON字符串
        String jsonResponse = objectMapper.writeValueAsString(data.getMsg());
        Map<String, Object> messageMap = (Map<String, Object>) data.getMsg();
        // 发送数据
        VideoParserUtil.sendData(channel, messageMap.get("command").toString(), jsonResponse);
    }
}
