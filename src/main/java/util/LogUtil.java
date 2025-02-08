package util;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: gateway-netty
 * @description: 日志工具类
 * @author: Havad
 * @create: 2025-02-08 09:41
 **/

@Slf4j
public class LogUtil {

    /**
     * 记录方法调用的信息日志
     * @param className 类名
     * @param methodName 方法名
     * @param params 方法参数
     * @param result 方法返回结果
     */
    public static void info(String className, String methodName, Object params, Object result) {
        log.info("[{}#{}] params:{}, result:{}",
                className, methodName, JSON.toJSONString(params), JSON.toJSONString(result));
    }

    /**
     * 记录方法调用的错误日志
     * @param className 类名
     * @param methodName 方法名
     * @param params 方法参数
     * @param e 异常信息
     */
    public static void error(String className, String methodName, Object params, Throwable e) {
        log.error("[{}#{}] params:{}, error message:{}",
                className, methodName, JSON.toJSONString(params), e.getMessage(), e);
    }
}
