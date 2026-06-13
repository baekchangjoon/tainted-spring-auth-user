package com.tainted.authuser.web.dto;

public record VerifyResponse(boolean active, String userId, String socialProvider) {}
