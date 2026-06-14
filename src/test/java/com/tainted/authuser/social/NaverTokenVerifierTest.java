package com.tainted.authuser.social;

import com.tainted.authuser.error.InvalidTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NaverTokenVerifierTest {

    private final SocialProperties.Provider config = new SocialProperties.Provider();

    @Test
    void userinfoYieldsExternalId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverTokenVerifier verifier = new NaverTokenVerifier(builder.build(), config);

        server.expect(requestTo(NaverTokenVerifier.USERINFO_URL))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer naver-access"))
                .andRespond(withSuccess(
                        "{\"resultcode\":\"00\",\"message\":\"success\",\"response\":{\"id\":\"naver-99\"}}",
                        APPLICATION_JSON));

        ExternalIdentity id = verifier.verify("naver", "naver-access");
        assertEquals("naver", id.provider());
        assertEquals("naver:naver-99", id.externalId());
        server.verify();
    }

    @Test
    void nonZeroResultCodeRejected() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverTokenVerifier verifier = new NaverTokenVerifier(builder.build(), config);

        server.expect(requestTo(NaverTokenVerifier.USERINFO_URL))
                .andRespond(withSuccess("{\"resultcode\":\"024\",\"message\":\"Authentication failed\"}",
                        APPLICATION_JSON));

        assertThrows(InvalidTokenException.class, () -> verifier.verify("naver", "bad-token"));
    }

    @Test
    void unauthorizedRejected() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverTokenVerifier verifier = new NaverTokenVerifier(builder.build(), config);

        server.expect(requestTo(NaverTokenVerifier.USERINFO_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(InvalidTokenException.class, () -> verifier.verify("naver", "expired"));
    }

    @Test
    void blankTokenRejected() {
        NaverTokenVerifier verifier = new NaverTokenVerifier(RestClient.builder().build(), config);
        assertThrows(InvalidTokenException.class, () -> verifier.verify("naver", ""));
    }
}
