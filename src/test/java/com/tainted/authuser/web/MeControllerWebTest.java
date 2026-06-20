package com.tainted.authuser.web;

import com.tainted.authuser.error.InvalidTokenException;
import com.tainted.authuser.error.UserNotFoundException;
import com.tainted.authuser.service.AuthService;
import com.tainted.authuser.web.dto.MeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MeController + GlobalExceptionHandler 슬라이스 테스트. Bearer 추출/401/404 분기 검증. */
@WebMvcTest(MeController.class)
class MeControllerWebTest {

    @Autowired MockMvc mvc;
    @MockBean AuthService authService;

    @Test
    void me_validBearer_returns200() throws Exception {
        when(authService.me("good-token"))
                .thenReturn(new MeResponse("u-1", "익명", "kakao"));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u-1"))
                .andExpect(jsonPath("$.socialProvider").value("kakao"))
                .andExpect(jsonPath("$.displayName").value("익명"));
    }

    @Test
    void me_missingHeader_returns401ProblemJson() throws Exception {
        // extractBearer 가 null 헤더에 대해 InvalidTokenException 던짐.
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Invalid token"))
                .andExpect(jsonPath("$.detail").value("missing bearer token"));
    }

    @Test
    void me_nonBearerScheme_returns401() throws Exception {
        mvc.perform(get("/api/v1/me").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("missing bearer token"));
    }

    @Test
    void me_invalidToken_returns401FromService() throws Exception {
        when(authService.me("expired"))
                .thenThrow(new InvalidTokenException("invalid or expired token"));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("invalid or expired token"));
    }

    @Test
    void me_userGone_returns404ProblemJson() throws Exception {
        when(authService.me("orphan"))
                .thenThrow(new UserNotFoundException("user not found: ghost"));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer orphan"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.detail").value("user not found: ghost"));
    }
}
