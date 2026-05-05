package com.yahzo.exchangeproxy.consumer;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahzo.exchangeproxy.config.ExchangeProxyProperties;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;

@Service
public class ExchangeRateConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateConsumer.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ElasticsearchClient elasticsearchClient;
    private final ExchangeProxyProperties properties;
    private final ObjectMapper objectMapper;

    public ExchangeRateConsumer(
            ElasticsearchClient elasticsearchClient,
            ExchangeProxyProperties properties,
            ObjectMapper objectMapper
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${exchange.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    // public void indexExchangeRates(String payload) {
    //     try {
    //         Map<String, Object> document = buildDocument(payload);
    //         IndexResponse response = elasticsearchClient.index(index -> index
    //                 .index(properties.elasticsearch().index())
    //                 .document(document));

    //         log.info("Donnee stockee dans Elasticsearch. index={}, result={}, id={}",
    //                 properties.elasticsearch().index(),
    //                 response.result(),
    //                 response.id());
    //     } catch (Exception exception) {
    //         log.error("Erreur lors de l'indexation Elasticsearch", exception);
    //     }
    // }
    public void indexExchangeRates(String payload) {
        try {
            Map<String, Object> document = buildDocument(payload);
            IndexResponse response = indexDocument(document);
            logSuccess(response);
        } catch (Exception exception) {
            logError(exception);
        }
    }

    private IndexResponse indexDocument(Map<String, Object> document) throws IOException {
        return elasticsearchClient.index(index -> index
                .index(getIndexName())
                .document(document));
    }

    private String getIndexName() {
        return properties.elasticsearch().index();
    }

    private void logSuccess(IndexResponse response) {
        log.info("Donnee stockee dans Elasticsearch. index={}, result={}, id={}",
                getIndexName(),
                response.result(),
                response.id());
    }

    private void logError(Exception exception) {
        log.error("Erreur lors de l'indexation Elasticsearch", exception);
    }

    private Map<String, Object> buildDocument(String json) throws IOException {
        JsonNode data = objectMapper.readTree(json);
        Map<String, Object> document = new LinkedHashMap<>();

        document.put("@timestamp", Instant.now().toString());
        document.put("timestamp", data.path("time_last_updated_utc").asText(""));
        document.put("base", data.path("base").isMissingNode() ? null : data.path("base").asText());
        document.put("rates", jsonNodeToMap(data.path("rates")));

        return document;
    }

    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Collections.emptyMap();
        }

        return objectMapper.convertValue(node, MAP_TYPE);
    }
}
