package com.tainted.authuser.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(@NotBlank String token) {}
