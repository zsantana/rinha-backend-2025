package com.rinha.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@RegisterForReflection
public record PaymentRequest(
    @NotNull
    UUID correlationId,

    @NotNull
    @Positive
    BigDecimal amount
) {}

