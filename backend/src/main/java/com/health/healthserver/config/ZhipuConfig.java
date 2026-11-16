package com.health.healthserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zhipu.ai")
public class ZhipuConfig {
    private String apiKey;
    private String apiUrl;
    private String model;
}