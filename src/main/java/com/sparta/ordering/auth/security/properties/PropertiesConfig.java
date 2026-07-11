package com.sparta.ordering.auth.security.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, TimeProperties.class})
public class PropertiesConfig {
}
