package com.tainted.authuser.social;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.tainted.authuser.error.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleIdTokenVerifierTest {

    private static final String CLIENT_ID = "test-client.apps.googleusercontent.com";
    private static final String KID = "test-kid";

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private GoogleIdTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        publicKey = (RSAPublicKey) kp.getPublic();
        privateKey = (RSAPrivateKey) kp.getPrivate();

        JwkProvider jwkProvider = mock(JwkProvider.class);
        Jwk jwk = mock(Jwk.class);
        when(jwk.getPublicKey()).thenReturn(publicKey);
        when(jwkProvider.get(KID)).thenReturn(jwk);

        verifier = new GoogleIdTokenVerifier(jwkProvider, CLIENT_ID);
    }

    private String sign(String issuer, String audience, String subject, Date expiresAt) {
        Algorithm alg = Algorithm.RSA256(publicKey, privateKey);
        return JWT.create()
                .withKeyId(KID)
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject(subject)
                .withExpiresAt(expiresAt)
                .sign(alg);
    }

    private Date future() {
        return new Date(System.currentTimeMillis() + 3_600_000);
    }

    @Test
    void validIdTokenYieldsExternalId() {
        String token = sign("https://accounts.google.com", CLIENT_ID, "1234567890", future());
        ExternalIdentity id = verifier.verify("google", token);
        assertEquals("google", id.provider());
        assertEquals("google:1234567890", id.externalId());
    }

    @Test
    void wrongAudienceRejected() {
        String token = sign("https://accounts.google.com", "other-client", "123", future());
        assertThrows(InvalidTokenException.class, () -> verifier.verify("google", token));
    }

    @Test
    void wrongIssuerRejected() {
        String token = sign("https://evil.example.com", CLIENT_ID, "123", future());
        assertThrows(InvalidTokenException.class, () -> verifier.verify("google", token));
    }

    @Test
    void expiredTokenRejected() {
        String token = sign("https://accounts.google.com", CLIENT_ID, "123",
                new Date(System.currentTimeMillis() - 1000));
        assertThrows(InvalidTokenException.class, () -> verifier.verify("google", token));
    }

    @Test
    void garbageTokenRejected() {
        assertThrows(InvalidTokenException.class, () -> verifier.verify("google", "not-a-jwt"));
    }

    @Test
    void blankTokenRejected() {
        assertThrows(InvalidTokenException.class, () -> verifier.verify("google", "  "));
    }
}
