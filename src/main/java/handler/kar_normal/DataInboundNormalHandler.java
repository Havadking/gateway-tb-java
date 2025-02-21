package handler.kar_normal;

import disruptor.DeviceDataEvent;
import disruptor.DeviceDataEventProducer;
import handler.DataInboundHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import model.DeviceData;
import protocol.ProtocolIdentifier;
import util.LogUtils;
import util.PDUUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @program: gateway-netty
 * @description: 进行数据处理的Handler
 * @author: Havad
 * @create: 2025-02-08 17:33
 **/

@AllArgsConstructor
public class DataInboundNormalHandler extends ChannelInboundHandlerAdapter implements DataInboundHandler {

    /**
     * 消息过期时间（毫秒）
     */
    private static final long EXPIRATION_TIME = 30 * 60 * 1000; // 30分钟

    /**
     * 清理间隔（毫秒）
     */
    private static final long CLEANUP_INTERVAL = 5 * 60 * 1000; // 5分钟

    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService scheduler;
    /**
     * 允许的最大未验证消息数
     */
    private static final int MAX_UNVALIDATED_MESSAGES = 100;
    /**
     * 设备数据事件生产者
     */
    private final DeviceDataEventProducer producer;

    /**
     * 设备ID与未验证消息列表的映射
     */
    private final Map<String, List<String>> deviceUnvalidatedMessages = new ConcurrentHashMap<>();

    /**
     * 设备ID与最后访问时间的映射
     */
    private final Map<String, Long> deviceLastAccessTime = new ConcurrentHashMap<>();


    /**
     * 构造函数
     *
     * @param producer 设备数据事件生产者
     */
    public DataInboundNormalHandler(DeviceDataEventProducer producer) {
        this.producer = producer;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // 启动定时清理任务
        startCleanupTask();
    }
    /**
     * 当从通道中读取到消息时被调用。
     *
     * <p>此方法首先通过PDUUtil的validateCheck方法验证接收到的消息，
     * 如果验证失败，则不处理该消息；
     * 如果验证成功，则将消息转换为DeviceData对象并将其放入Disruptor队列中，
     * 同时记录相应的日志信息。
     *
     * @param ctx 通道处理上下文
     * @param msg 从通道中读取到的消息对象
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        handleData(ctx, msg);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Override
    public void handleData(ChannelHandlerContext ctx, Object msg) throws Exception {
        String deviceId = ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).get();
        if (!PDUUtil.validateCheck((String) msg)) {
            LogUtils.logBusiness("普通话机消息验证失败，应该是话机上传短信{}", msg);

            // 更新设备最后访问时间
            deviceLastAccessTime.put(deviceId, System.currentTimeMillis());

            // 获取设备的未验证消息列表，如果不存在则创建
            List<String> deviceMessages = deviceUnvalidatedMessages.computeIfAbsent(
                    deviceId, k -> new ArrayList<>());

            // 检查列表大小，如果超过最大值，删除最旧的消息
            if (deviceMessages.size() >= MAX_UNVALIDATED_MESSAGES) {
                deviceMessages.remove(0);
                LogUtils.logBusiness("设备{}的未验证消息列表已达最大容量，删除最旧消息", deviceId);
            }

            // 添加消息到列表
            deviceMessages.add((String) msg);
            LogUtils.logBusiness("普通话机消息验证失败，已添加到设备{}的未验证消息列表，当前列表大小: {}",
                    deviceId, deviceMessages.size());
        } else {
            // 直接将Message放到Disruptor队列中
            int funcNo = PDUUtil.getFuncNo((String) msg);
            LogUtils.logBusiness("普通话机的任务号为{}", funcNo);
            if (funcNo == 98) {
                List<String> deviceMessages = deviceUnvalidatedMessages.get(deviceId);

                if (deviceMessages != null && !deviceMessages.isEmpty()) {
                    LogUtils.logBusiness("设备{}上传短信，将长度为{}的之前未验证数据拼接",
                            deviceId, deviceMessages.size());

                    // 将设备的所有未验证消息拼接到当前消息后面
                    StringBuilder concatenatedMessage = new StringBuilder(String.valueOf(msg));
                    for (String unvalidatedMsg : deviceMessages) {
                        concatenatedMessage.append(unvalidatedMsg);
                    }

                    // 更新消息内容
                    msg = concatenatedMessage.toString();

                    // 清空设备的未验证消息列表
                    deviceMessages.clear();
                    LogUtils.logBusiness("已清空设备{}的未验证消息列表,", deviceId);
                }
            }
            LogUtils.logBusiness("普通话机数据{}写入Disruptor", msg);
            DeviceData data = new DeviceData(
                    PDUUtil.getDeviceNo((String) msg),
                    msg,
                    ProtocolIdentifier.PROTOCOL_NORMAL);
            producer.onData(data, DeviceDataEvent.Type.TO_TB);
        }
    }
    /**
     * 启动定时清理任务
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                LogUtils.logBusiness("开始执行未验证消息清理任务，当前设备数：{}", deviceLastAccessTime.size());

                // 清理超过过期时间的设备消息
                deviceLastAccessTime.entrySet().removeIf(entry -> {
                    String deviceId = entry.getKey();
                    long lastAccess = entry.getValue();

                    if (currentTime - lastAccess > EXPIRATION_TIME) {
                        deviceUnvalidatedMessages.remove(deviceId);
                        LogUtils.logBusiness("清理设备{}的未验证消息，该设备已{}分钟未活动",
                                deviceId, (currentTime - lastAccess) / (60 * 1000));
                        return true;
                    }
                    return false;
                });
            } catch (Exception e) {
                LogUtils.logError("清理未验证消息任务执行异常", e);
            }
        }, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * 关闭处理器，释放资源
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public void close() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 清理所有设备的消息
        deviceUnvalidatedMessages.clear();
        deviceLastAccessTime.clear();
        LogUtils.logBusiness("DataInboundNormalHandler关闭，清理所有资源");
    }

    /**
     * 当通道关闭时调用，清理相关资源
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 获取设备ID
        String deviceId = ctx.channel().attr(AttributeKey.<String>valueOf("deviceId")).get();
        if (deviceId != null) {
            // 清理设备相关的资源
            deviceUnvalidatedMessages.remove(deviceId);
            deviceLastAccessTime.remove(deviceId);
            LogUtils.logBusiness("设备{}连接断开，清理相关资源", deviceId);
        }
        super.channelInactive(ctx);
    }
}
