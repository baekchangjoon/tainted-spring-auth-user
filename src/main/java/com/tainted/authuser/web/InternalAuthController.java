package com.tainted.authuser.web;

import com.tainted.authuser.service.AuthService;
import com.tainted.authuser.web.dto.UserResponse;
import com.tainted.authuser.web.dto.VerifyRequest;
import com.tainted.authuser.web.dto.VerifyResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
public class InternalAuthController {

    private final AuthService authService;

    public InternalAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/verify")
    public VerifyResponse verify(@Valid @RequestBody VerifyRequest request) {
        return authService.verify(request.token());
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable String id) {
        return authService.getUser(id);
    }
}
