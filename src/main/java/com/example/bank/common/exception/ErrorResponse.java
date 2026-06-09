package com.example.bank.common.exception;

import java.time.Instant;
import java.util.UUID;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    UUID traceId
) {}
