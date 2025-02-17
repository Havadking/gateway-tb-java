package util;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: 卡尔视频话机协议名称和编码的映射
 * @author: Havad
 * @create: 2025-02-14 19:48
 **/

public class ProtocolTypeMapper {
    private static final Map<String, byte[]> methodProtocolMap = new HashMap<>();

    static {
        // 7.1 平台通用协议
        methodProtocolMap.put("link", new byte[]{0x00, 0x00}); // 联机协议没有明确的协议类型，这里假设为0x00, 0x00
        methodProtocolMap.put("heartbeat", new byte[]{0x03, 0x13}); // 心跳
        methodProtocolMap.put("devstatus", new byte[]{0x03, (byte) 0xa7}); // 设备状态通用上报
        methodProtocolMap.put("setAuthorize", new byte[]{0x03, 0x7f}); // 终端授权
        methodProtocolMap.put("getAuthorize", new byte[]{0x03, (byte) 0xaf}); // 查询授权状态
        methodProtocolMap.put("readlog", new byte[]{0x03, (byte) 0xb3}); // 读取终端日志

        // 7.2 终端维护子系统
        methodProtocolMap.put("deviceLog", new byte[]{0x03, 0x17}); // 通用终端上报日志
        methodProtocolMap.put("reqfee", new byte[]{0x03, 0x11}); // 计费话机请求余额和费率
        methodProtocolMap.put("call", new byte[]{0x03, 0x12}); // 话机上报通话记录
        methodProtocolMap.put("setConfigInfo", new byte[]{0x03, (byte) 0x82}); // 家校通配置内容下发
        methodProtocolMap.put("getConfigInfo", new byte[]{0x03, (byte) 0x83}); // 家校通配置内容获取
        methodProtocolMap.put("feedback", new byte[]{0x03, 0x53}); // 终端任务反馈
        methodProtocolMap.put("verifyNum", new byte[]{0x23, 0x28}); // 1.1 从亲情号号码池校验手机号
        methodProtocolMap.put("querySIM", new byte[]{0x23, 0x29});    // 1.3 SIM卡套餐余量查询
        methodProtocolMap.put("notice", new byte[]{0x03, 0x30});     // 1.5 推送广告、公告、通知等
        methodProtocolMap.put("getFamilyNumList", new byte[]{0x03, (byte) 0x80});     // 根据卡号获取亲情号码
    }

    public static byte[] getProtocolTypeByMethod(String method) {
        return methodProtocolMap.getOrDefault(method, null);
    }
}
