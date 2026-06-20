package com.tainted.authuser.token;

import com.tainted.authuser.id.IdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** TokenService 단위 테스트. StringRedisTemplate/ValueOperations 모킹으로 외부 Redis 없이 검증. */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> ops;
    @Mock IdGenerator idGenerator;

    @Test
    void issue_storesTokenWithUserIdAndTtl() {
        when(idGenerator.newId()).thenReturn("tok-1");
        when(redis.opsForValue()).thenReturn(ops);
        TokenService svc = new TokenService(redis, idGenerator, 3600);

        String token = svc.issue("user-1");

        assertEquals("tok-1", token);
        verify(ops).set("auth:token:tok-1", "user-1", Duration.ofSeconds(3600));
    }

    @Test
    void resolveUserId_returnsStoredUserId() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("auth:token:tok-1")).thenReturn("user-1");
        TokenService svc = new TokenService(redis, idGenerator, 3600);

        assertEquals(Optional.of("user-1"), svc.resolveUserId("tok-1"));
    }

    @Test
    void resolveUserId_returnsEmptyWhenMissing() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("auth:token:ghost")).thenReturn(null);
        TokenService svc = new TokenService(redis, idGenerator, 3600);

        assertTrue(svc.resolveUserId("ghost").isEmpty());
    }

    @Test
    void resolveUserId_nullToken_returnsEmptyWithoutHittingRedis() {
        TokenService svc = new TokenService(redis, idGenerator, 3600);
        assertTrue(svc.resolveUserId(null).isEmpty());
        verifyNoInteractions(redis);
    }

    @Test
    void resolveUserId_blankToken_returnsEmptyWithoutHittingRedis() {
        TokenService svc = new TokenService(redis, idGenerator, 3600);
        assertTrue(svc.resolveUserId("   ").isEmpty());
        verifyNoInteractions(redis);
    }
}
