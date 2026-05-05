package com.yahzo.exchangeproxy.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic exchangeRatesTopic(ExchangeProxyProperties properties) {
        return TopicBuilder.name(properties.kafka().topic())
                .partitions(properties.kafka().partitions())
                .replicas(properties.kafka().replicationFactor())
                .build();
    }
}
