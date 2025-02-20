package config;

import lombok.Getter;
import redis.clients.jedis.JedisPooled;

/**
 * @program: gateway-netty
 * @description: Redis 配置类
 * @author: Havad
 * @create: 2025-02-17 16:30
 **/

public class RedisConfig {
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_USER = null;
    private static final String REDIS_PASSWORD = null;

    @Getter
    private static JedisPooled jedisPool;

    static {
        if (REDIS_PASSWORD != null && REDIS_USER != null) {
            jedisPool = new JedisPooled(REDIS_HOST, REDIS_PORT, REDIS_USER, REDIS_PASSWORD);
        } else {
            jedisPool = new JedisPooled(REDIS_HOST, REDIS_PORT);
        }
    }

    // 关闭 redis 连接池
    public static void closePool() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

}
