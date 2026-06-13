package com.tainted.authuser.social;

public interface SocialVerifier {
    /** providerToken 을 검증하고 외부 신원을 반환. 실패 시 InvalidTokenException. */
    ExternalIdentity verify(String provider, String providerToken);
}
