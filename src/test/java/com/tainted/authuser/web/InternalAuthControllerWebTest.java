package com.tainted.authuser.web;

import com.tainted.authuser.error.UserNotFoundException;
import com.tainted.authuser.service.AuthService;
import com.tainted.authuser.web.dto.UserResponse;
import com.tainted.authuser.web.dto.VerifyResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** InternalAuthController + GlobalExceptionHandler 슬라이스 테스트. verify/getUser 분기 검증. */
@WebMvcTest(InternalAuthController.class)
class InternalAuthControllerWebTest {

    @Autowired MockMvc mvc;
    @MockBean AuthService authService;

    @Test
    void verify_active_returns200() throws Exception {
        when(authService.verify("tok-1"))
                .thenReturn(new VerifyResponse(true, "u-1", "naver"));

        mvc.perform(post("/internal/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.userId").value("u-1"))
                .andExpect(jsonPath("$.socialProvider").value("naver"));
    }

    @Test
    void verify_inactive_returns200WithNulls() throws Exception {
        when(authService.verify("ghost"))
                .thenReturn(new VerifyResponse(false, null, null));

        mvc.perform(post("/internal/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"ghost\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void verify_blankToken_returns400() throws Exception {
        // VerifyRequest.token @NotBlank 위반.
        mvc.perform(post("/internal/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void getUser_found_returns200() throws Exception {
        when(authService.getUser("u-1"))
                .thenReturn(new UserResponse("u-1", "kakao", "2026-01-01T00:00:00Z"));

        mvc.perform(get("/internal/users/u-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u-1"))
                .andExpect(jsonPath("$.socialProvider").value("kakao"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"));
    }

    @Test
    void getUser_missing_returns404ProblemJson() throws Exception {
        when(authService.getUser("nope"))
                .thenThrow(new UserNotFoundException("user not found: nope"));

        mvc.perform(get("/internal/users/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.detail").value("user not found: nope"));
    }
}
