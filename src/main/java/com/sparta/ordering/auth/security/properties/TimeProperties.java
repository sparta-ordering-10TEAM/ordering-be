package com.sparta.ordering.auth.security.properties;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

@ConfigurationProperties(prefix = "time")
@Getter
@RequiredArgsConstructor
public class TimeProperties {
    private final ZoneId zoneId;
}
