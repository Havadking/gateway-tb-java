package kafka;

import model.DeviceData;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: gateway-netty
 * @description: kafka 配置类
 * @author: Havad
 * @create: 2025-02-27 15:08
 **/

@Configuration
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

    // 配置生产者
    @Bean
    public ProducerFactory<String, DeviceData> deviceDataProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, DeviceData> deviceDataKafkaTemplate() {
        return new KafkaTemplate<>(deviceDataProducerFactory());
    }

    // 配置消费者
    @Bean
    public ConsumerFactory<String, DeviceData> deviceDataConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new JsonDeserializer<>(DeviceData.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceData> deviceDataKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DeviceData> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deviceDataConsumerFactory());
        return factory;
    }

}
