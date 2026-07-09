package com.sparta.ordering.ai.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String url,
        String apiKey
) {
}
