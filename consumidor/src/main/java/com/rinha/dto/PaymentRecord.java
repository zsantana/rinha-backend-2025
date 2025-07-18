package com.rinha.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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