package nz.co.westpac.poc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import nz.co.westpac.poc.api.dto.AccountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AccountCacheTest {

    private static final String ACCOUNT_ID = "a".repeat(64);
    private static final String KEY = "accounts:" + ACCOUNT_ID;
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final AccountResponse ACCOUNT =
            new AccountResponse(ACCOUNT_ID, "03 0187 1234567 000", "Hwan Kim", "holiday");

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AccountCache cache;

    @BeforeEach
    void setUp() {
        cache = new AccountCache(redisTemplate, JsonMapper.builder().build(), TTL);
    }

    @Test
    void returnsEmptyWhenTheAccountIsNotCached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn(null);

        assertThat(cache.get(ACCOUNT_ID)).isEmpty();
    }

    @Test
    void returnsTheCachedAccount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("""
                {"accountId":"%s","accountNumber":"03 0187 1234567 000","customerName":"Hwan Kim","accountNickname":"holiday"}
                """.formatted(ACCOUNT_ID));

        assertThat(cache.get(ACCOUNT_ID)).contains(ACCOUNT);
    }

    @Test
    void returnsEmptyWhenTheCachedValueIsNotReadable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("not json");

        assertThat(cache.get(ACCOUNT_ID)).isEmpty();
    }

    @Test
    void returnsEmptyWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(cache.get(ACCOUNT_ID)).isEmpty();
    }

    @Test
    void storesTheAccountWithTheConfiguredTimeToLive() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.put(ACCOUNT_ID, ACCOUNT);

        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq(KEY),
                org.mockito.ArgumentMatchers.contains(ACCOUNT_ID), org.mockito.ArgumentMatchers.eq(TTL));
    }

    @Test
    void swallowsRedisFailuresWhileStoring() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> cache.put(ACCOUNT_ID, ACCOUNT)).doesNotThrowAnyException();
    }

    @Test
    void keepsAMissingNicknameThroughTheRoundTrip() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("""
                {"accountId":"%s","accountNumber":"03 0187 1234567 000","customerName":"Hwan Kim","accountNickname":null}
                """.formatted(ACCOUNT_ID));

        assertThat(cache.get(ACCOUNT_ID))
                .hasValueSatisfying(account -> assertThat(account.accountNickname()).isNull());
    }
}
