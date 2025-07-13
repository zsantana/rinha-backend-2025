package com.rinha.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
    @NotNull
    @JsonProperty("correlationId")
    UUID correlationId,

    @NotNull
    @Positive
    @JsonProperty("amount")
    BigDecimal amount
) {}
