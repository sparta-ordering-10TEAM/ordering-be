package com.sparta.ordering.ai.config;

import com.sparta.ordering.ai.client.GeminiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiClientConfig {
}
