package com.tainted.authuser.social;

import com.tainted.authuser.error.InvalidTokenException;
import org.springframework.stereotype.Component;
import java.util.Set;

/**
 * 결정론적 소셜 검증 mock. 외부 호출 없음.
 * 유효 토큰 형식: "valid-<provider>-<seed>"  →  externalId = "<provider>:<seed>".
 */
@Component
public class MockSocialVerifier implements SocialVerifier {

    private static final Set<String> SUPPORTED = Set.of("google", "kakao", "naver", "toss");

    @Override
    public ExternalIdentity verify(String provider, String providerToken) {
        if (!SUPPORTED.contains(provider)) {
            throw new InvalidTokenException("unsupported provider: " + provider);
        }
        String prefix = "valid-" + provider + "-";
        if (providerToken == null || !providerToken.startsWith(prefix)) {
            throw new InvalidTokenException("malformed token for provider: " + provider);
        }
        String seed = providerToken.substring(prefix.length());
        if (seed.isBlank()) {
            throw new InvalidTokenException("empty seed");
        }
        return new ExternalIdentity(provider, provider + ":" + seed);
    }
}
