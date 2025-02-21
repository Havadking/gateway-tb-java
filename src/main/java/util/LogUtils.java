package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: gateway-netty
 * @description: 日志类
 * @author: Havad
 * @create: 2025-02-15 15:00
 **/

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class LogUtils {
    /**
     * 错误阈值
     */
    private static final int ERROR_THRESHOLD = 100;
    /**
     * 错误计数器
     */
    private static final AtomicInteger ERROR_COUNT = new AtomicInteger(0);


    /**
     * 业务日志记录器
     */
    private static final Logger BUSINESS_LOGGER = LogManager.getLogger("BusinessLog");
    /**
     * 错误日志记录器
     */
    private static final Logger ERROR_LOGGER = LogManager.getLogger("ErrorLog");
    /**
     * 性能日志记录器
     */
    private static final Logger PERFORMANCE_LOGGER = LogManager.getLogger("com.gateway.performance");

    /**
     * 记录业务日志的方法
     *
     * @param message 日志信息模板
     * @param params  日志信息参数
     */// 业务日志方法
    public static void logBusiness(String message, Object... params) {
        BUSINESS_LOGGER.info(message, params);
    }

    /**
     * 记录错误日志的方法。
     *
     * @param message 错误信息
     * @param e       异常对象
     * @param params  动态参数，用于格式化错误信息
     */// 错误日志方法
    public static void logError(String message, Throwable e, Object... params) {
        ERROR_LOGGER.error(message, params, e);

        if (ERROR_COUNT.incrementAndGet() > ERROR_THRESHOLD) {
            // 发送告警通知，可以集成邮件、短信或其他告警系统
            // todo 错误过多，发送短信警告
            ERROR_COUNT.set(0);
        }
    }

    /**
     * 记录性能日志的方法
     *
     * @param operation     执行的操作名称
     * @param executionTime 执行操作所需的时间（毫秒）
     */
    public static void logPerformance(String operation, long executionTime) {
        PERFORMANCE_LOGGER.info("操作: {}, 执行时间: {}ms", operation, executionTime);
    }


}
