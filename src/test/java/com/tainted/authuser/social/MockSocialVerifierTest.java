package com.tainted.authuser.social;

import com.tainted.authuser.error.InvalidTokenException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MockSocialVerifierTest {

    private final MockSocialVerifier verifier = new MockSocialVerifier();

    @Test
    void validTokenYieldsDeterministicExternalId() {
        ExternalIdentity a = verifier.verify("kakao", "valid-kakao-u1");
        ExternalIdentity b = verifier.verify("kakao", "valid-kakao-u1");
        assertEquals("kakao", a.provider());
        assertEquals("kakao:u1", a.externalId());
        assertEquals(a.externalId(), b.externalId(), "같은 입력은 같은 externalId");
    }

    @Test
    void googleNowSupportedInMock() {
        ExternalIdentity id = verifier.verify("google", "valid-google-u1");
        assertEquals("google", id.provider());
        assertEquals("google:u1", id.externalId());
    }

    @Test
    void unsupportedProviderRejected() {
        assertThrows(InvalidTokenException.class,
                () -> verifier.verify("facebook", "valid-facebook-u1"));
    }

    @Test
    void malformedTokenRejected() {
        assertThrows(InvalidTokenException.class,
                () -> verifier.verify("kakao", "garbage"));
    }
}
