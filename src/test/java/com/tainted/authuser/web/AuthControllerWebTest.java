package com.tainted.authuser.web;

import com.tainted.authuser.error.InvalidTokenException;
import com.tainted.authuser.service.AuthService;
import com.tainted.authuser.web.dto.LoginRequest;
import com.tainted.authuser.web.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController + GlobalExceptionHandler 슬라이스 테스트(@WebMvcTest + MockMvc).
 * 실제 HTTP 상태/바디를 검증한다. Spring problemdetails.enabled=true 이므로 검증 실패는
 * 프레임워크 기본 핸들러가 application/problem+json 400 으로 처리(커스텀 핸들러를 가림).
 */
@WebMvcTest(AuthController.class)
class AuthControllerWebTest {

    @Autowired MockMvc mvc;
    @MockBean AuthService authService;

    @Test
    void login_validRequest_returns200WithBody() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("tok-1", "익명"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"kakao\",\"providerToken\":\"valid-kakao-u1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("tok-1"))
                .andExpect(jsonPath("$.displayName").value("익명"));
    }

    @Test
    void login_blankProvider_returns400ProblemJson() throws Exception {
        // @NotBlank 위반 → MethodArgumentNotValidException → 400 problem+json.
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"\",\"providerToken\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_invalidToken_returns401ProblemJson() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidTokenException("unsupported provider: facebook"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"facebook\",\"providerToken\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid token"))
                .andExpect(jsonPath("$.detail").value("unsupported provider: facebook"));
    }

    @Test
    void guest_returns200() throws Exception {
        when(authService.guest()).thenReturn(new LoginResponse("g-tok", "익명"));

        mvc.perform(post("/api/v1/auth/guest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("g-tok"));
    }
}
