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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** AuthService 순수 단위 테스트(Mockito). 협력자(repo/verifier/token/id/clock)를 모두 모킹. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserAccountRepository repository;
    @Mock SocialVerifier socialVerifier;
    @Mock TokenService tokenService;
    @Mock IdGenerator idGenerator;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(repository, socialVerifier, tokenService, idGenerator, fixedClock);
    }

    @Test
    void login_existingUser_reusesAccountAndIssuesToken() {
        when(socialVerifier.verify("kakao", "tok"))
                .thenReturn(new ExternalIdentity("kakao", "kakao:42"));
        UserAccount existing = new UserAccount("u-1", "kakao", "kakao:42", Instant.now(fixedClock));
        when(repository.findBySocialProviderAndExternalId("kakao", "kakao:42"))
                .thenReturn(Optional.of(existing));
        when(tokenService.issue("u-1")).thenReturn("token-1");

        LoginResponse resp = service.login(new LoginRequest("kakao", "tok"));

        assertEquals("token-1", resp.accessToken());
        assertEquals(AuthService.DISPLAY_NAME, resp.displayName());
        verify(repository, never()).save(any());
    }

    @Test
    void login_newUser_savesAccountWithGeneratedId() {
        when(socialVerifier.verify("naver", "tok"))
                .thenReturn(new ExternalIdentity("naver", "naver:99"));
        when(repository.findBySocialProviderAndExternalId("naver", "naver:99"))
                .thenReturn(Optional.empty());
        when(idGenerator.newId()).thenReturn("new-id");
        when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issue("new-id")).thenReturn("token-2");

        LoginResponse resp = service.login(new LoginRequest("naver", "tok"));

        assertEquals("token-2", resp.accessToken());
        verify(repository).save(argThat(u ->
                u.getId().equals("new-id")
                        && u.getSocialProvider().equals("naver")
                        && u.getExternalId().equals("naver:99")
                        && u.getCreatedAt().equals(Instant.parse("2026-01-01T00:00:00Z"))));
    }

    @Test
    void login_verifierRejects_propagatesException() {
        when(socialVerifier.verify("kakao", "bad"))
                .thenThrow(new InvalidTokenException("malformed"));
        assertThrows(InvalidTokenException.class,
                () -> service.login(new LoginRequest("kakao", "bad")));
        verifyNoInteractions(tokenService);
    }

    @Test
    void guest_createsGuestAccountWithGuestExternalId() {
        when(idGenerator.newId()).thenReturn("g-id", "g-seed");
        when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issue("g-id")).thenReturn("g-token");

        LoginResponse resp = service.guest();

        assertEquals("g-token", resp.accessToken());
        assertEquals(AuthService.DISPLAY_NAME, resp.displayName());
        verify(repository).save(argThat(u ->
                u.getId().equals("g-id")
                        && u.getSocialProvider().equals("guest")
                        && u.getExternalId().equals("guest:g-seed")));
    }

    @Test
    void me_validToken_returnsProfile() {
        when(tokenService.resolveUserId("t")).thenReturn(Optional.of("u-1"));
        when(repository.findById("u-1"))
                .thenReturn(Optional.of(new UserAccount("u-1", "google", "google:1", Instant.now(fixedClock))));

        MeResponse me = service.me("t");

        assertEquals("u-1", me.userId());
        assertEquals("google", me.socialProvider());
        assertEquals(AuthService.DISPLAY_NAME, me.displayName());
    }

    @Test
    void me_unknownToken_throwsInvalidToken() {
        when(tokenService.resolveUserId("t")).thenReturn(Optional.empty());
        InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> service.me("t"));
        assertTrue(ex.getMessage().contains("invalid or expired"));
    }

    @Test
    void me_tokenValidButUserGone_throwsUserNotFound() {
        when(tokenService.resolveUserId("t")).thenReturn(Optional.of("ghost"));
        when(repository.findById("ghost")).thenReturn(Optional.empty());
        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> service.me("t"));
        assertTrue(ex.getMessage().contains("ghost"));
    }

    @Test
    void verify_activeWhenTokenResolvesToUser() {
        when(tokenService.resolveUserId("t")).thenReturn(Optional.of("u-1"));
        when(repository.findById("u-1"))
                .thenReturn(Optional.of(new UserAccount("u-1", "kakao", "kakao:1", Instant.now(fixedClock))));

        VerifyResponse v = service.verify("t");

        assertTrue(v.active());
        assertEquals("u-1", v.userId());
        assertEquals("kakao", v.socialProvider());
    }

    @Test
    void verify_inactiveWhenTokenUnknown() {
        when(tokenService.resolveUserId("ghost")).thenReturn(Optional.empty());
        VerifyResponse v = service.verify("ghost");
        assertFalse(v.active());
        assertNull(v.userId());
        assertNull(v.socialProvider());
    }

    @Test
    void verify_inactiveWhenTokenResolvesButUserMissing() {
        when(tokenService.resolveUserId("t")).thenReturn(Optional.of("u-x"));
        when(repository.findById("u-x")).thenReturn(Optional.empty());
        VerifyResponse v = service.verify("t");
        assertFalse(v.active());
        assertNull(v.userId());
    }

    @Test
    void getUser_returnsUserResponseWithCreatedAtAsString() {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        when(repository.findById("u-1"))
                .thenReturn(Optional.of(new UserAccount("u-1", "naver", "naver:5", created)));

        UserResponse u = service.getUser("u-1");

        assertEquals("u-1", u.id());
        assertEquals("naver", u.socialProvider());
        assertEquals(created.toString(), u.createdAt());
    }

    @Test
    void getUser_missing_throwsUserNotFound() {
        when(repository.findById("nope")).thenReturn(Optional.empty());
        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> service.getUser("nope"));
        assertTrue(ex.getMessage().contains("nope"));
    }
}
