package com.tainted.authuser.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 직접 단위 테스트. ProblemDetail 상태/타이틀/디테일을 직접 검증한다.
 * 특히 검증 핸들러의 "에러 없음" 폴백 분기는 슬라이스 테스트로 재현하기 어려워 여기서 직접 커버.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidToken_maps401() {
        ProblemDetail pd = handler.handleInvalidToken(new InvalidTokenException("bad token"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), pd.getStatus());
        assertEquals("Invalid token", pd.getTitle());
        assertEquals("bad token", pd.getDetail());
    }

    @Test
    void userNotFound_maps404() {
        ProblemDetail pd = handler.handleUserNotFound(new UserNotFoundException("no user x"));
        assertEquals(HttpStatus.NOT_FOUND.value(), pd.getStatus());
        assertEquals("User not found", pd.getTitle());
        assertEquals("no user x", pd.getDetail());
    }

    @Test
    void validation_withFieldError_usesFirstDefaultMessage() throws Exception {
        BindingResult br = new BeanPropertyBindingResult(new Object(), "loginRequest");
        br.addError(new FieldError("loginRequest", "provider", "must not be blank"));
        MethodParameter param = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, br);

        ProblemDetail pd = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
        assertEquals("Validation failed", pd.getTitle());
        assertEquals("must not be blank", pd.getDetail());
    }

    @Test
    void validation_withNoErrors_fallsBackToInvalidRequest() {
        // getAllErrors().isEmpty() == true 분기(폴백 메시지).
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = new BeanPropertyBindingResult(new Object(), "obj");
        when(ex.getBindingResult()).thenReturn(br);

        ProblemDetail pd = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
        assertEquals("invalid request", pd.getDetail());
    }
}
