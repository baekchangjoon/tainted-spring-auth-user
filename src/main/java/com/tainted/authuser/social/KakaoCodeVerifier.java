package com.tainted.authuser.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.tainted.authuser.error.InvalidTokenException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Kakao Authorization Code 검증기. providerToken 은 카카오 로그인으로 받은 인가 code.
 * 서버에서 code 를 access token 으로 교환(client_secret 사용)한 뒤 user/me 로 식별자를 얻는다.
 * 두 번의 외부 호출(token, userinfo)이 일어나는 코드 경로다.
 */
public class KakaoCodeVerifier implements SocialVerifier {

    static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    static final String USERINFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final SocialProperties.Provider config;

    public KakaoCodeVerifier(RestClient restClient, SocialProperties.Provider config) {
        this.restClient = restClient;
        this.config = config;
    }

    @Override
    public ExternalIdentity verify(String provider, String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            throw new InvalidTokenException("empty kakao authorization code");
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", config.getClientId());
            if (config.getClientSecret() != null && !config.getClientSecret().isBlank()) {
                form.add("client_secret", config.getClientSecret());
            }
            form.add("redirect_uri", config.getRedirectUri());
            form.add("code", providerToken);

            JsonNode tokenResponse = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            String accessToken = tokenResponse == null ? null
                    : tokenResponse.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new InvalidTokenException("kakao token exchange returned no access_token");
            }

            JsonNode me = restClient.get()
                    .uri(USERINFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            String id = me == null ? null : me.path("id").asText(null);
            if (id == null || id.isBlank() || "null".equals(id)) {
                throw new InvalidTokenException("kakao user/me returned no id");
            }
            return new ExternalIdentity("kakao", "kakao:" + id);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("kakao verification failed: " + e.getMessage());
        }
    }
}
