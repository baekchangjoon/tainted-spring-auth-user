package com.tainted.authuser.config;

import com.auth0.jwk.GuavaCachedJwkProvider;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.tainted.authuser.social.CompositeSocialVerifier;
import com.tainted.authuser.social.GoogleIdTokenVerifier;
import com.tainted.authuser.social.KakaoCodeVerifier;
import com.tainted.authuser.social.MockSocialVerifier;
import com.tainted.authuser.social.NaverTokenVerifier;
import com.tainted.authuser.social.SocialProperties;
import com.tainted.authuser.social.SocialVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * 소셜 로그인 빈 와이어링. provider별 실제 검증기를 구성하고 {@link CompositeSocialVerifier}로
 * 묶어 @Primary {@link SocialVerifier} 로 노출한다(AuthService 가 이 빈을 주입받음).
 */
@Configuration
@EnableConfigurationProperties(SocialProperties.class)
public class SocialConfig {

    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    /** Kakao/Naver userinfo 호출용 HTTP 클라이언트(타임아웃 지정). */
    @Bean
    public RestClient socialRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        return RestClient.builder().requestFactory(factory).build();
    }

    /** Google JWKS(공개키) 캐시 제공자. */
    @Bean
    public JwkProvider googleJwkProvider() {
        try {
            return new GuavaCachedJwkProvider(new UrlJwkProvider(new URL(GOOGLE_JWKS_URL)));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("invalid google jwks url", e);
        }
    }

    @Bean
    @Primary
    public SocialVerifier socialVerifier(SocialProperties props, MockSocialVerifier mock,
                                         JwkProvider googleJwkProvider, RestClient socialRestClient) {
        SocialVerifier google = new GoogleIdTokenVerifier(googleJwkProvider, props.getGoogle().getClientId());
        SocialVerifier kakao = new KakaoCodeVerifier(socialRestClient, props.getKakao());
        SocialVerifier naver = new NaverTokenVerifier(socialRestClient, props.getNaver());
        return new CompositeSocialVerifier(props, mock, google, kakao, naver);
    }
}
