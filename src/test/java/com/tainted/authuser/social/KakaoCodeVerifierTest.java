package com.tainted.authuser.social;

import com.tainted.authuser.error.InvalidTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KakaoCodeVerifierTest {

    private SocialProperties.Provider config() {
        SocialProperties.Provider p = new SocialProperties.Provider();
        p.setClientId("kakao-rest-key");
        p.setClientSecret("kakao-secret");
        p.setRedirectUri("http://localhost:3000/callback");
        return p;
    }

    @Test
    void codeExchangedThenUserinfoYieldsExternalId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoCodeVerifier verifier = new KakaoCodeVerifier(builder.build(), config());

        server.expect(requestTo(KakaoCodeVerifier.TOKEN_URL))
                .andExpect(method(POST))
                .andRespond(withSuccess("{\"access_token\":\"atk-1\"}", APPLICATION_JSON));
        server.expect(requestTo(KakaoCodeVerifier.USERINFO_URL))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer atk-1"))
                .andRespond(withSuccess("{\"id\":1234567}", APPLICATION_JSON));

        ExternalIdentity id = verifier.verify("kakao", "auth-code");
        assertEquals("kakao", id.provider());
        assertEquals("kakao:1234567", id.externalId());
        server.verify();
    }

    @Test
    void tokenExchangeFailureRejected() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoCodeVerifier verifier = new KakaoCodeVerifier(builder.build(), config());

        server.expect(requestTo(KakaoCodeVerifier.TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body("{\"error\":\"invalid_grant\"}").contentType(APPLICATION_JSON));

        assertThrows(InvalidTokenException.class, () -> verifier.verify("kakao", "bad-code"));
    }

    @Test
    void missingAccessTokenRejected() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KakaoCodeVerifier verifier = new KakaoCodeVerifier(builder.build(), config());

        server.expect(requestTo(KakaoCodeVerifier.TOKEN_URL))
                .andRespond(withSuccess("{\"token_type\":\"bearer\"}", APPLICATION_JSON));

        assertThrows(InvalidTokenException.class, () -> verifier.verify("kakao", "code"));
    }

    @Test
    void blankCodeRejected() {
        KakaoCodeVerifier verifier = new KakaoCodeVerifier(RestClient.builder().build(), config());
        assertThrows(InvalidTokenException.class, () -> verifier.verify("kakao", " "));
    }
}
