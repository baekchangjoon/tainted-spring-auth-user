package com.tainted.authuser.social;

import com.tainted.authuser.error.InvalidTokenException;

/**
 * provider 값으로 분기하는 소셜 검증기. provider별 mode(mock|real)에 따라
 * 실제 OAuth 검증기 또는 결정론적 mock 으로 위임한다(의도적 이기종 검증 경로).
 *
 * <ul>
 *   <li>google → OIDC ID-token JWKS 검증</li>
 *   <li>kakao  → Authorization code 교환 + user/me</li>
 *   <li>naver  → access token + nid/me 인트로스펙션</li>
 *   <li>toss   → 항상 mock (B2B 제휴 필요로 실연동 제외)</li>
 * </ul>
 */
public class CompositeSocialVerifier implements SocialVerifier {

    private final SocialProperties props;
    private final SocialVerifier mock;
    private final SocialVerifier google;
    private final SocialVerifier kakao;
    private final SocialVerifier naver;

    public CompositeSocialVerifier(SocialProperties props, SocialVerifier mock,
                                   SocialVerifier google, SocialVerifier kakao, SocialVerifier naver) {
        this.props = props;
        this.mock = mock;
        this.google = google;
        this.kakao = kakao;
        this.naver = naver;
    }

    @Override
    public ExternalIdentity verify(String provider, String providerToken) {
        return switch (provider == null ? "" : provider) {
            case "google" -> pick("google", google).verify(provider, providerToken);
            case "kakao" -> pick("kakao", kakao).verify(provider, providerToken);
            case "naver" -> pick("naver", naver).verify(provider, providerToken);
            case "toss" -> mock.verify(provider, providerToken);
            default -> throw new InvalidTokenException("unsupported provider: " + provider);
        };
    }

    private SocialVerifier pick(String provider, SocialVerifier real) {
        return "real".equalsIgnoreCase(props.modeOf(provider)) ? real : mock;
    }
}
