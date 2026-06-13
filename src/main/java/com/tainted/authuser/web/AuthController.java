package com.tainted.authuser.web;

import com.tainted.authuser.service.AuthService;
import com.tainted.authuser.web.dto.LoginRequest;
import com.tainted.authuser.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/guest")
    public LoginResponse guest() {
        return authService.guest();
    }
}
