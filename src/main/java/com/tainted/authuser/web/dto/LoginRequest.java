package com.tainted.authuser.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String provider, @NotBlank String providerToken) {}
