package util;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 卡尔视频话机协议名称和编码的映射
 * @author: Havad
 * @create: 2025-02-14 19:48
 **/

@SuppressWarnings({"checkstyle:HideUtilityClassConstructor", "checkstyle:MagicNumber"})
public class ProtocolTypeMapper {
    /**
     * 方法协议映射表。
     * 存储了方法名与对应协议数据的映射关系。
     */
    private static final Map<String, byte[]> METHOD_PROTOCOL_MAP = new HashMap<>();

    static {
        // 7.1 平台通用协议
        METHOD_PROTOCOL_MAP.put("link", new byte[]{0x00, 0x00}); // 联机协议没有明确的协议类型，这里假设为0x00, 0x00
        METHOD_PROTOCOL_MAP.put("heartbeat", new byte[]{0x03, 0x13}); // 心跳
        METHOD_PROTOCOL_MAP.put("devstatus", new byte[]{0x03, (byte) 0xa7}); // 设备状态通用上报
        METHOD_PROTOCOL_MAP.put("setAuthorize", new byte[]{0x03, 0x7f}); // 终端授权
        METHOD_PROTOCOL_MAP.put("getAuthorize", new byte[]{0x03, (byte) 0xaf}); // 查询授权状态
        METHOD_PROTOCOL_MAP.put("readlog", new byte[]{0x03, (byte) 0xb3}); // 读取终端日志

        // 7.2 终端维护子系统
        METHOD_PROTOCOL_MAP.put("deviceLog", new byte[]{0x03, 0x17}); // 通用终端上报日志
        METHOD_PROTOCOL_MAP.put("reqfee", new byte[]{0x03, 0x11}); // 计费话机请求余额和费率
        METHOD_PROTOCOL_MAP.put("call", new byte[]{0x03, 0x12}); // 话机上报通话记录
        METHOD_PROTOCOL_MAP.put("setConfigInfo", new byte[]{0x03, (byte) 0x82}); // 家校通配置内容下发
        METHOD_PROTOCOL_MAP.put("getConfigInfo", new byte[]{0x03, (byte) 0x83}); // 家校通配置内容获取
        METHOD_PROTOCOL_MAP.put("feedback", new byte[]{0x03, 0x53}); // 终端任务反馈
        METHOD_PROTOCOL_MAP.put("verifyNum", new byte[]{0x23, 0x28}); // 1.1 从亲情号号码池校验手机号
        METHOD_PROTOCOL_MAP.put("querySIM", new byte[]{0x23, 0x29});    // 1.3 SIM卡套餐余量查询
        METHOD_PROTOCOL_MAP.put("notice", new byte[]{0x03, 0x30});     // 1.5 推送广告、公告、通知等
        METHOD_PROTOCOL_MAP.put("getFamilyNumList", new byte[]{0x03, (byte) 0x80});     // 根据卡号获取亲情号码
    }

    /**
     * 根据给定方法名获取协议类型
     *
     * @param method 方法名
     * @return 对应的协议类型字节码数组，如果未找到则返回null
     */
    public static byte[] getProtocolTypeByMethod(String method) {
        return METHOD_PROTOCOL_MAP.getOrDefault(method, null);
    }
}
