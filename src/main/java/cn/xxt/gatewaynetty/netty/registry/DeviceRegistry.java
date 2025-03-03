package cn.xxt.gatewaynetty.netty.registry;

import cn.xxt.gatewaynetty.mqtt.MqttSender;
import cn.xxt.gatewaynetty.util.LogUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: 设备注册管理
 * @author: Havad
 * @create: 2025-02-07 17:21
 **/

@AllArgsConstructor
@Component
public class DeviceRegistry {
    /**
     * 设备映射表，使用ConcurrentHashMap确保线程安全
     */
    private final ConcurrentHashMap<String, Channel> tcpChannels = new ConcurrentHashMap<>();

    /**
     * 使用ConcurrentHashMap存储的HTTP通道集合。
     * 键为字符串，代表通道的标识；值为Channel对象，代表具体的HTTP通道。
     */
    private final ConcurrentHashMap<String, Channel> httpChannels = new ConcurrentHashMap<>();


    /**
     * HTTP连接计数器
     */
    private final AtomicInteger httpConnectionCount = new AtomicInteger(0);

    /**
     * MQTT消息发送器
     */
    private MqttSender mqttSender;

    /**
     * 注册设备与通道的映射关系。
     *
     * @param deviceId 设备ID
     * @param channel  通道对象
     */
    public void register(String deviceId, Channel channel) {
        LogUtils.logBusiness("设备注册{}", deviceId);
        tcpChannels.put(deviceId, channel);
        channel.attr(AttributeKey.<String>valueOf("deviceId")).set(deviceId);
        // 注册成功后，向Thingsboard声明设备通过网关上线
        mqttSender.sendDeviceConnected(deviceId);

        // 添加 ChannelFutureListener 来监听连接关闭事件
        channel.closeFuture().addListener((ChannelFutureListener) future -> unregister(deviceId));
    }


    /**
     * 注册HTTP通道。
     *
     * @param deviceId    设备ID
     * @param httpChannel HTTP通道
     */
    public void registerHttpChannel(String deviceId, Channel httpChannel) {
        LogUtils.logBusiness("卡尔视频话机建立 HTTP 连接:{}", deviceId);
        httpChannels.put(deviceId, httpChannel);

        // 增加 HTTP 连接计数
        int count = httpConnectionCount.incrementAndGet();
        LogUtils.logBusiness("当前 HTTP 连接数: {}", count);

        // 添加 ChannelFutureListener 来监听连接关闭事件
        httpChannel.closeFuture().addListener((ChannelFutureListener) future -> unregisterHttp(deviceId));
    }


    /**
     * 根据设备ID获取对应的通道。
     *
     * @param deviceId 设备ID
     * @return 对应的通道对象，如果不存在则返回null
     */
    public Channel getChannel(String deviceId) {
        return tcpChannels.get(deviceId);
    }

    /**
     * 注销设备。
     *
     * @param deviceId 设备ID
     */
    public void unregister(String deviceId) {
        LogUtils.logBusiness("设备注销{}", deviceId);
        // 移除所有 TCP 和 HTTP 连接
        Channel tcpChannel = tcpChannels.remove(deviceId);
        if (tcpChannel != null) {
            tcpChannel.close();
        }

        Channel httpChannel = httpChannels.remove(deviceId);
        if (httpChannel != null) {
            httpChannel.close();
        }
        // 向Thingsboard声明设备断连
        mqttSender.sendDeviceDisconnected(deviceId);
    }

    /**
     * 注销设备的HTTP连接
     *
     * @param deviceId 设备ID标识
     */
    public void unregisterHttp(String deviceId) {
        LogUtils.logBusiness("设备Http链接关闭{}", deviceId);
        Channel httpChannel = httpChannels.remove(deviceId);
        // 减少 HTTP 连接计数
        int count = httpConnectionCount.decrementAndGet();
        LogUtils.logBusiness("当前 HTTP 连接数: {}", count);

        if (httpChannel != null) {
            httpChannel.close();
        }
    }

}
