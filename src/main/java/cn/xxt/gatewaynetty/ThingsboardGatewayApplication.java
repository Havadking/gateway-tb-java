package cn.xxt.gatewaynetty;

import cn.xxt.gatewaynetty.netty.ThingsboardGateway;
import cn.xxt.gatewaynetty.util.LogUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description:
 * @author: Havad
 * @create: 2025-02-28 10:36
 **/
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@SpringBootApplication
@EnableConfigurationProperties
public class ThingsboardGatewayApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ThingsboardGatewayApplication.class, args);

        // 从Spring Context获取ThingsboardGateway实例并启动
        ThingsboardGateway gateway = context.getBean(ThingsboardGateway.class);
        try {
            gateway.start();
        } catch (Exception e) {
            LogUtils.logError("ThingsboardGateway启动失败", e);
        }
    }
}
