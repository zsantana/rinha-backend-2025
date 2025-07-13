package com.rinha.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRecord(
    UUID correlationId,
    BigDecimal amount,
    Instant requestedAt,
    ProcessorType processorType
) {
    public enum ProcessorType {
        DEFAULT, FALLBACK
    }
}