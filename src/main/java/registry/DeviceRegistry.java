package registry;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import mqtt.MqttSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: gateway-netty
 * @description: 设备注册管理
 * @author: Havad
 * @create: 2025-02-07 17:21
 **/

@AllArgsConstructor
public class DeviceRegistry {
    private static final Logger log = LoggerFactory.getLogger(DeviceRegistry.class);
    /**
     * 设备映射表，使用ConcurrentHashMap确保线程安全
     */
    private final ConcurrentHashMap<String, Channel> deviceMap = new ConcurrentHashMap<>();

    private MqttSender mqttSender;

    /**
     * 注册设备与通道的映射关系。
     *
     * @param deviceId 设备ID
     * @param channel  通道对象
     */
    public void register(String deviceId, Channel channel) {
        log.info("设备注册{}", deviceId);
        deviceMap.put(deviceId, channel);
        channel.attr(AttributeKey.<String>valueOf("deviceId")).set(deviceId);
        // 注册成功后，向Thingsboard声明设备通过网关上线
        mqttSender.sendDeviceConnected(deviceId);

        // 添加 ChannelFutureListener 来监听连接关闭事件
        channel.closeFuture().addListener((ChannelFutureListener) future -> unregister(deviceId));
    }

    /**
     * 根据设备ID获取对应的通道。
     *
     * @param deviceId 设备ID
     * @return 对应的通道对象，如果不存在则返回null
     */
    public Channel getChannel(String deviceId) {
        return deviceMap.get(deviceId);
    }

    /**
     * 注销设备。
     *
     * @param deviceId 设备ID
     */
    public void unregister(String deviceId) {
        log.info("设备注销{}", deviceId);
        deviceMap.remove(deviceId);
        // 向Thingsboard声明设备断连
        mqttSender.sendDeviceDisconnected(deviceId);
    }

}
