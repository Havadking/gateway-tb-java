package gataway.config;

import lombok.Getter;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

/**
 * @program: gateway-netty
 * @description: Redis 配置类
 * @author: Havad
 * @create: 2025-02-17 16:30
 **/

@SuppressWarnings({"checkstyle:HideUtilityClassConstructor", "checkstyle:JavadocVariable", "checkstyle:FinalClass"})
public class RedisConfig {
    /**
     * Redis服务的宿主机地址。
     */
    private static final String REDIS_HOST = "192.168.3.101";
    /**
     * Redis服务的端口号。
     */
    private static final int REDIS_PORT = 6387;
    /**
     * 用户Redis缓存的前缀
     */
    private static final String REDIS_USER = null;
    /**
     * Redis的密码。
     * 初始值为null，需要在配置时设置实际的密码。
     */
    private static final String REDIS_PASSWORD = "xxt123XXT";

    /**
     * Redis数据库索引编号。
     */
    private static final int REDIS_DATABASE = 7;

    /**
     * Jedis 客户端连接池
     */
    @Getter
    private static JedisPooled jedisPool;

    static {
        HostAndPort hostAndPort = new HostAndPort(REDIS_HOST, REDIS_PORT);
        JedisClientConfig config = DefaultJedisClientConfig.builder()
                .password(REDIS_PASSWORD)
                .database(REDIS_DATABASE)
                .build();
        jedisPool = new JedisPooled(hostAndPort, config);
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

    /**
     * 私有构造函数，防止外部实例化该工具类。
     */
    private RedisConfig() {
        throw new IllegalStateException("Utility class");
    }

}
