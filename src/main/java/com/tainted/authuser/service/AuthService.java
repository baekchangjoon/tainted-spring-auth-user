package com.tainted.authuser.service;

import com.tainted.authuser.domain.UserAccount;
import com.tainted.authuser.domain.UserAccountRepository;
import com.tainted.authuser.error.InvalidTokenException;
import com.tainted.authuser.error.UserNotFoundException;
import com.tainted.authuser.id.IdGenerator;
import com.tainted.authuser.social.ExternalIdentity;
import com.tainted.authuser.social.SocialVerifier;
import com.tainted.authuser.token.TokenService;
import com.tainted.authuser.web.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;

@Service
public class AuthService {

    public static final String DISPLAY_NAME = "익명";

    private final UserAccountRepository repository;
    private final SocialVerifier socialVerifier;
    private final TokenService tokenService;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public AuthService(UserAccountRepository repository, SocialVerifier socialVerifier,
                       TokenService tokenService, IdGenerator idGenerator, Clock clock) {
        this.repository = repository;
        this.socialVerifier = socialVerifier;
        this.tokenService = tokenService;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        ExternalIdentity identity = socialVerifier.verify(request.provider(), request.providerToken());
        UserAccount user = repository
                .findBySocialProviderAndExternalId(identity.provider(), identity.externalId())
                .orElseGet(() -> repository.save(new UserAccount(
                        idGenerator.newId(), identity.provider(), identity.externalId(), Instant.now(clock))));
        String token = tokenService.issue(user.getId());
        return new LoginResponse(token, DISPLAY_NAME);
    }

    @Transactional
    public LoginResponse guest() {
        UserAccount user = repository.save(new UserAccount(
                idGenerator.newId(), "guest", "guest:" + idGenerator.newId(), Instant.now(clock)));
        String token = tokenService.issue(user.getId());
        return new LoginResponse(token, DISPLAY_NAME);
    }

    @Transactional(readOnly = true)
    public MeResponse me(String token) {
        String userId = tokenService.resolveUserId(token)
                .orElseThrow(() -> new InvalidTokenException("invalid or expired token"));
        UserAccount user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("user not found: " + userId));
        return new MeResponse(user.getId(), DISPLAY_NAME, user.getSocialProvider());
    }

    @Transactional(readOnly = true)
    public VerifyResponse verify(String token) {
        return tokenService.resolveUserId(token)
                .flatMap(repository::findById)
                .map(u -> new VerifyResponse(true, u.getId(), u.getSocialProvider()))
                .orElse(new VerifyResponse(false, null, null));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(String id) {
        UserAccount user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("user not found: " + id));
        return new UserResponse(user.getId(), user.getSocialProvider(), user.getCreatedAt().toString());
    }
}
