package com.yahzo.exchangeproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange")
public record ExchangeProxyProperties(
        Kafka kafka,
        String apiUrl,
        long fetchIntervalMs,
        Elasticsearch elasticsearch
) {
    public record Kafka(String topic, int partitions, short replicationFactor) {
    }

    public record Elasticsearch(String url, String index) {
    }
}
