package com.yahzo.exchangeproxy.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yahzo.exchangeproxy.config.ExchangeProxyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ExchangeRateProducer {
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ExchangeProxyProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ExchangeRateProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ExchangeProxyProperties properties,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Scheduled(fixedDelayString = "${exchange.fetch-interval-ms}", initialDelay = 1000)
    public void publishLatestRates() {
        try {
            String payload = fetchExchangeRates();
            JsonNode data = objectMapper.readTree(payload);
            String base = data.path("base").asText("USD");

            kafkaTemplate.send(properties.kafka().topic(), base, payload);
            log.info("Taux envoyes dans Kafka. topic={}, base={}", properties.kafka().topic(), base);
        } catch (Exception exception) {
            log.error("Erreur lors de l'envoi des taux dans Kafka", exception);
        }
    }

    private String fetchExchangeRates() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.apiUrl()))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("API HTTP " + response.statusCode());
        }

        return response.body();
    }
}
