package com.yahzo.exchangeproxy.web;

import com.yahzo.exchangeproxy.config.ExchangeProxyProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatusController {

    private final ExchangeProxyProperties properties;

    public StatusController(ExchangeProxyProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/")
    public Map<String, Object> status() {
        return Map.of(
                "application", "exchange-proxy",
                "topic", properties.kafka().topic(),
                "elasticsearchIndex", properties.elasticsearch().index(),
                "fetchIntervalMs", properties.fetchIntervalMs()
        );
    }
}
