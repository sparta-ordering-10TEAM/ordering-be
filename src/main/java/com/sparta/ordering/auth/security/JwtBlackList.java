package com.sparta.ordering.auth.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtBlackList {

    /**
     * key = access token / value = expiration time
     */
    private Map<String, Instant> blackList = new ConcurrentHashMap();

    public void addBlackList(String accessToken, Instant expirationTime) {
        blackList.put(accessToken, expirationTime);
    }

    public boolean existsInBlacklist(String accessToken) {
        return blackList.containsKey(accessToken);
    }

    // 메모리 누수 방지를 위해 1시간마다 만료된 액세스 토큰 삭제
    @Scheduled(cron = "0 0 0/1 * * *")
    public void cleanExpiredToken() {
        log.info("Clean JwtBlacklist: delete expired access tokens in list");
        Instant now = Instant.now();
        blackList.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

}
