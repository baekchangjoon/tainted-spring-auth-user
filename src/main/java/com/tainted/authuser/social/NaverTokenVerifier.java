package com.tainted.authuser.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.tainted.authuser.error.InvalidTokenException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Naver access-token 검증기. providerToken 은 네이버 로그인 SDK 가 발급한 access token.
 * 서버에서 nid/me(userinfo) 를 호출해 식별자를 얻는다(토큰 인트로스펙션식, 외부 호출 1회).
 */
public class NaverTokenVerifier implements SocialVerifier {

    static final String USERINFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final RestClient restClient;

    public NaverTokenVerifier(RestClient restClient, SocialProperties.Provider config) {
        this.restClient = restClient;
        // config 는 향후 확장(서명검증 등)을 위해 보존하나 현재 access-token 흐름에선 미사용.
    }

    @Override
    public ExternalIdentity verify(String provider, String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            throw new InvalidTokenException("empty naver access token");
        }
        try {
            JsonNode body = restClient.get()
                    .uri(USERINFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerToken)
                    .retrieve()
                    .body(JsonNode.class);

            String resultCode = body == null ? null : body.path("resultcode").asText(null);
            if (!"00".equals(resultCode)) {
                throw new InvalidTokenException("naver nid/me resultcode != 00: " + resultCode);
            }
            String id = body.path("response").path("id").asText(null);
            if (id == null || id.isBlank() || "null".equals(id)) {
                throw new InvalidTokenException("naver nid/me returned no id");
            }
            return new ExternalIdentity("naver", "naver:" + id);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("naver verification failed: " + e.getMessage());
        }
    }
}
