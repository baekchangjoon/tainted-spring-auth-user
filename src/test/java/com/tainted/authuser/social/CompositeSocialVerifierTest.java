package com.tainted.authuser.social;

import com.tainted.authuser.error.InvalidTokenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompositeSocialVerifierTest {

    private final SocialProperties props = new SocialProperties();
    private final MockSocialVerifier mock = new MockSocialVerifier();
    // 실제 검증기 대역: 호출되면 "<provider>:REAL" 을 돌려준다.
    private final SocialVerifier google = (p, t) -> new ExternalIdentity("google", "google:REAL");
    private final SocialVerifier kakao = (p, t) -> new ExternalIdentity("kakao", "kakao:REAL");
    private final SocialVerifier naver = (p, t) -> new ExternalIdentity("naver", "naver:REAL");

    private CompositeSocialVerifier composite() {
        return new CompositeSocialVerifier(props, mock, google, kakao, naver);
    }

    @Test
    void defaultsToMockPerProvider() {
        // 기본 mode=mock → 실제 검증기 대신 mock 으로 라우팅
        assertEquals("google:u1", composite().verify("google", "valid-google-u1").externalId());
        assertEquals("kakao:u2", composite().verify("kakao", "valid-kakao-u2").externalId());
        assertEquals("naver:u3", composite().verify("naver", "valid-naver-u3").externalId());
    }

    @Test
    void realModeRoutesToRealVerifier() {
        props.getGoogle().setMode("real");
        props.getKakao().setMode("REAL"); // 대소문자 무시
        assertEquals("google:REAL", composite().verify("google", "any").externalId());
        assertEquals("kakao:REAL", composite().verify("kakao", "any").externalId());
        // naver 는 여전히 mock
        assertEquals("naver:u3", composite().verify("naver", "valid-naver-u3").externalId());
    }

    @Test
    void tossAlwaysMockEvenIfModeReal() {
        // toss 는 실제 검증기가 없어 항상 mock
        assertEquals("toss:s1", composite().verify("toss", "valid-toss-s1").externalId());
    }

    @Test
    void unsupportedProviderRejected() {
        assertThrows(InvalidTokenException.class, () -> composite().verify("facebook", "x"));
    }
}
