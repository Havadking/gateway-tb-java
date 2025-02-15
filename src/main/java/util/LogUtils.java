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

public class LogUtils {
    private static final int ERROR_THRESHOLD = 100;
    private static final AtomicInteger errorCount = new AtomicInteger(0);


    private static final Logger businessLogger = LogManager.getLogger("BusinessLog");
    private static final Logger errorLogger = LogManager.getLogger("ErrorLog");
    private static final Logger performanceLogger = LogManager.getLogger("com.gateway.performance");

    // 业务日志方法
    public static void logBusiness(String message, Object... params) {
        businessLogger.info(message, params);
    }

    // 错误日志方法
    public static void logError(String message, Throwable e, Object... params) {
        errorLogger.error(message, params, e);

        if (errorCount.incrementAndGet() > ERROR_THRESHOLD) {
            // 发送告警通知，可以集成邮件、短信或其他告警系统
            // todo 错误过多，发送短信警告
            errorCount.set(0);
        }
    }

    // 性能日志方法
    public static void logPerformance(String operation, long executionTime) {
        performanceLogger.info("操作: {}, 执行时间: {}ms", operation, executionTime);
    }


}
