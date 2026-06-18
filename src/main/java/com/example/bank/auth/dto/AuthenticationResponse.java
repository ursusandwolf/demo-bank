package com.example.bank.auth.dto;

public record AuthenticationResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
