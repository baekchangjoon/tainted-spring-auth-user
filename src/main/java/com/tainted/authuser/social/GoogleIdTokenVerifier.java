package com.tainted.authuser.social;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.tainted.authuser.error.InvalidTokenException;

import java.security.interfaces.RSAPublicKey;

/**
 * Google OIDC ID-token 검증기. providerToken 은 Google 이 발급한 ID token(JWT).
 * Google JWKS 에서 kid 에 해당하는 공개키를 받아 서명을 검증하고, aud(=clientId)와
 * iss 를 확인한 뒤 sub 로 외부 신원을 구성한다. 외부 네트워크는 JWKS 키 조회뿐(캐시).
 */
public class GoogleIdTokenVerifier implements SocialVerifier {

    private final JwkProvider jwkProvider;
    private final String clientId;

    public GoogleIdTokenVerifier(JwkProvider jwkProvider, String clientId) {
        this.jwkProvider = jwkProvider;
        this.clientId = clientId;
    }

    @Override
    public ExternalIdentity verify(String provider, String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            throw new InvalidTokenException("empty google id token");
        }
        try {
            DecodedJWT decoded = JWT.decode(providerToken);
            Jwk jwk = jwkProvider.get(decoded.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            DecodedJWT verified = JWT.require(algorithm)
                    .withIssuer("https://accounts.google.com", "accounts.google.com")
                    .withAudience(clientId)
                    .build()
                    .verify(providerToken);
            String sub = verified.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new InvalidTokenException("google id token has no subject");
            }
            return new ExternalIdentity("google", "google:" + sub);
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("google id token verification failed: " + e.getMessage());
        }
    }
}
