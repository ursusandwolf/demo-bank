package com.example.bank.auth.dto;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 72) String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {
    @com.fasterxml.jackson.annotation.JsonIgnore
    @AssertTrue(message = "Passwords must not exceed 72 UTF-8 bytes")
    public boolean isWithinBcryptLimit() {
        return withinLimit(currentPassword) && withinLimit(newPassword);
    }

    private static boolean withinLimit(String value) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
