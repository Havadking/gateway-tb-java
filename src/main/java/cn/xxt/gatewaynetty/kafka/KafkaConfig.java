package cn.xxt.gatewaynetty.kafka;

import cn.xxt.gatewaynetty.netty.model.DeviceData;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-cn.xxt.gatewaynetty.netty
 * @description: kafka 配置类
 * @author: Havad
 * @create: 2025-02-27 15:08
 **/

@Configuration
@EnableKafka
@SuppressWarnings("checkstyle:MagicNumber")
public class KafkaConfig {
    /**
     * Kafka服务器的地址列表，用于建立初始连接。
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    /**
     * Kafka消费者组ID
     */
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    /**
     * Kafka发送数据到Thingsboard的主题常量
     */
    public static final String TO_TB_TOPIC = "device-data-to-tb";
    /**
     * 设备数据主题，用于发送数据到设备。
     */
    public static final String TO_DEVICE_TOPIC = "device-data-to-device";

    /**
     * 创建名为TO_TB的主题Bean
     *
     * @return NewTopic对象，用于创建具有指定分区数和副本因子的主题
     */
    @Bean
    public NewTopic toTbTopic() {
        return new NewTopic(TO_TB_TOPIC, 3, (short) 1);
    }

    /**
     * 创建发送至设备主题的Bean方法
     *
     * @return NewTopic对象，包含主题名称、分区数和副本数
     */
    @Bean
    public NewTopic toDeviceTopic() {
        return new NewTopic(TO_DEVICE_TOPIC, 3, (short) 1);
    }


    /**
     * Kafka Admin客户端配置方法
     *
     * @return KafkaAdmin 实例，用于管理Kafka集群
     */// Kafka Admin客户端配置
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }


    /**
     * 创建设备数据生产者工厂的Bean方法
     *
     * @return Kafka生产者工厂实例，用于处理字符串键和设备数据值的消息
     */// 配置生产者
    @Bean
    public ProducerFactory<String, DeviceData> deviceDataProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * 创建一个用于发送设备数据的Kafka模板
     *
     * @return KafkaTemplate对象，用于处理字符串键和DeviceData值的Kafka消息
     */
    @Bean
    public KafkaTemplate<String, DeviceData> deviceDataKafkaTemplate() {
        return new KafkaTemplate<>(deviceDataProducerFactory());
    }


    /**
     * 创建用于处理设备数据的Kafka消费者工厂
     *
     * @return Kafka消费者工厂实例，用于消费字符串键和设备数据类型的消息
     */// 配置消费者
    @Bean
    public ConsumerFactory<String, DeviceData> deviceDataConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Don't set VALUE_DESERIALIZER_CLASS_CONFIG here
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        // Create the deserializer directly
        JsonDeserializer<DeviceData> jsonDeserializer = new JsonDeserializer<>(DeviceData.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    /**
     * 创建并发Kafka监听器容器工厂方法
     *
     * @return 用于监听设备数据的Kafka监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceData> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeviceData> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deviceDataConsumerFactory());
        factory.setConcurrency(3); // 设置并发消费者数量
        return factory;
    }

}
