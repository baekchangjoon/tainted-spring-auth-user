package com.tainted.authuser.web;

import com.tainted.authuser.error.InvalidTokenException;
import com.tainted.authuser.service.AuthService;
import com.tainted.authuser.web.dto.MeResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public MeResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.me(extractBearer(authorization));
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new InvalidTokenException("missing bearer token");
        }
        return authorization.substring("Bearer ".length());
    }
}
