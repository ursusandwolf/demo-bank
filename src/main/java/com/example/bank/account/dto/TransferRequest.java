package com.example.bank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Запрос на перевод средств.
 */
public record TransferRequest(
        @NotBlank(message = "Source account number is required")
        @jakarta.validation.constraints.Size(max = 20)
        String fromAccountNumber,

        @NotBlank(message = "Destination account number is required")
        @jakarta.validation.constraints.Size(max = 20)
        String toAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @jakarta.validation.constraints.Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @jakarta.validation.constraints.Size(max = 255) String description
) {
}
