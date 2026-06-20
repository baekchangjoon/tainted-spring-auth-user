package com.tainted.authuser.domain;

import com.tainted.authuser.error.InvalidTokenException;
import com.tainted.authuser.error.UserNotFoundException;
import com.tainted.authuser.social.ExternalIdentity;
import com.tainted.authuser.social.SocialProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** 도메인/값 객체/예외/프로퍼티의 작은 단위 테스트(접근자/생성자/분기). */
class UserAccountTest {

    @Test
    void accessorsReturnConstructorValues() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UserAccount u = new UserAccount("id-1", "kakao", "kakao:7", now);
        assertEquals("id-1", u.getId());
        assertEquals("kakao", u.getSocialProvider());
        assertEquals("kakao:7", u.getExternalId());
        assertEquals(now, u.getCreatedAt());
    }

    @Test
    void externalIdentityRecordComponents() {
        ExternalIdentity ei = new ExternalIdentity("naver", "naver:1");
        assertEquals("naver", ei.provider());
        assertEquals("naver:1", ei.externalId());
    }

    @Test
    void exceptionsCarryMessage() {
        assertEquals("x", new InvalidTokenException("x").getMessage());
        assertEquals("y", new UserNotFoundException("y").getMessage());
    }

    @Test
    void socialProperties_modeOf_allBranches() {
        SocialProperties p = new SocialProperties();
        p.getGoogle().setMode("real");
        p.getKakao().setMode("mock");
        p.getNaver().setMode("real");
        assertEquals("real", p.modeOf("google"));
        assertEquals("mock", p.modeOf("kakao"));
        assertEquals("real", p.modeOf("naver"));
        assertEquals("mock", p.modeOf("unknown"));
        assertEquals("mock", p.modeOf(null));
    }

    @Test
    void socialProperties_providerAccessorsAndSetters() {
        SocialProperties p = new SocialProperties();
        SocialProperties.Provider g = new SocialProperties.Provider();
        g.setMode("real");
        g.setClientId("cid");
        g.setClientSecret("secret");
        g.setRedirectUri("uri");
        p.setGoogle(g);
        SocialProperties.Provider k = new SocialProperties.Provider();
        p.setKakao(k);
        SocialProperties.Provider n = new SocialProperties.Provider();
        p.setNaver(n);

        assertSame(g, p.getGoogle());
        assertSame(k, p.getKakao());
        assertSame(n, p.getNaver());
        assertEquals("real", g.getMode());
        assertEquals("cid", g.getClientId());
        assertEquals("secret", g.getClientSecret());
        assertEquals("uri", g.getRedirectUri());
    }
}
