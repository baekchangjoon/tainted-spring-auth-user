package com.tainted.authuser.token;

import com.tainted.authuser.id.IdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Optional;

@Service
public class TokenService {

    private static final String PREFIX = "auth:token:";

    private final StringRedisTemplate redis;
    private final IdGenerator idGenerator;
    private final long ttlSeconds;

    public TokenService(StringRedisTemplate redis,
                        IdGenerator idGenerator,
                        @Value("${auth.token-ttl-seconds}") long ttlSeconds) {
        this.redis = redis;
        this.idGenerator = idGenerator;
        this.ttlSeconds = ttlSeconds;
    }

    /** userId 에 대한 새 토큰을 발급하고 Redis 에 TTL 과 함께 저장. */
    public String issue(String userId) {
        String token = idGenerator.newId();
        redis.opsForValue().set(PREFIX + token, userId, Duration.ofSeconds(ttlSeconds));
        return token;
    }

    /** 토큰에 매핑된 userId 를 조회. 없거나 만료면 empty. */
    public Optional<String> resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(redis.opsForValue().get(PREFIX + token));
    }
}
