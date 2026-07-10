package com.sparta.ordering.auth.security.properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {
    @Bean
    public Clock clock(TimeProperties properties) {
        return Clock.system(properties.getZoneId());
    }
}
