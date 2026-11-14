package nz.co.westpac.poc.service;

import java.time.Duration;
import java.util.Optional;
import nz.co.westpac.poc.api.dto.AccountResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AccountCache {

    private static final String KEY_PREFIX = "accounts:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration timeToLive;

    public AccountCache(StringRedisTemplate redisTemplate,
                        ObjectMapper objectMapper,
                        @Value("${spring.cache.redis.time-to-live:10m}") Duration timeToLive) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.timeToLive = timeToLive;
    }

    public Optional<AccountResponse> get(String accountId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + accountId);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, AccountResponse.class));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public void put(String accountId, AccountResponse account) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + accountId,
                    objectMapper.writeValueAsString(account),
                    timeToLive);
        } catch (RuntimeException ex) {
            // cache failures must not fail the request
        }
    }
}
