package util;

/**
 * @program: gateway-netty
 * @description: 用于处理视频话机协议格式的工具类
 * @author: Havad
 * @create: 2025-02-14 17:26
 **/

public class VideoParserUtil {
    /**
     * 根据命令获取消息类型
     *
     * @param command 命令字符串
     * @return 对应的消息类型，如果命令为null或不在预定义列表中返回"response"，否则返回"request"
     */
    public static String getToDeviceMessageType(String command) {
        if (command == null) {
            return "response";
        }

        switch (command) {
            case "setConfigInfo":
            case "getConfigInfo":
            case "querySIM":
            case "verifyNum":
                return "request";
            default:
                return "response";
        }
    }

    /**
     * 根据命令获取对应的ToTB消息类型
     *
     * @param command 指令字符串
     * @return 对应的ToTB消息类型，"request"或"response"
     */
    public static String getToTBMessageType(String command) {
        if (command == null) {
            return "request";
        }

        switch (command) {
            case "setConfigInfo":
            case "getConfigInfo":
            case "querySIM":
            case "verifyNum":
                return "response";
            default:
                return "request";
        }
    }
}
