package config;

import lombok.Getter;
import redis.clients.jedis.JedisPooled;

/**
 * @program: gateway-netty
 * @description: Redis 配置类
 * @author: Havad
 * @create: 2025-02-17 16:30
 **/

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class RedisConfig {
    /**
     * Redis服务的宿主机地址。
     */
    private static final String REDIS_HOST = "localhost";
    /**
     * Redis服务的端口号。
     */
    private static final int REDIS_PORT = 6379;
    /**
     * 用户Redis缓存的前缀
     */
    private static final String REDIS_USER = null;
    /**
     * Redis的密码。
     * 初始值为null，需要在配置时设置实际的密码。
     */
    private static final String REDIS_PASSWORD = null;

    /**
     * Jedis 客户端连接池
     */
    @Getter
    private static JedisPooled jedisPool;

    static {
        if (REDIS_PASSWORD != null && REDIS_USER != null) {
            jedisPool = new JedisPooled(REDIS_HOST, REDIS_PORT, REDIS_USER, REDIS_PASSWORD);
        } else {
            jedisPool = new JedisPooled(REDIS_HOST, REDIS_PORT);
        }
    }

    /**
     * 关闭Redis连接池
     * <p>
     * 此方法用于关闭当前的Redis连接池，释放资源。
     */
    public static void closePool() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

}
