package protocol.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.Channel;
import model.DeviceData;

/**
 * @program: gateway-netty
 * @description:
 * @author: Havad
 * @create: 2025-02-15 09:21
 **/

public interface TcpMessageSender {
    void sendMessageToDevice(DeviceData data, Channel channel) throws Exception;
}
