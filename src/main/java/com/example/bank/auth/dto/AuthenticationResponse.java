package com.example.bank.auth.dto;

public record AuthenticationResponse(String accessToken, String tokenType, long expiresIn) {}
