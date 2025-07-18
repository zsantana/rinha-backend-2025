package com.rinha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

@RegisterForReflection
public record ProcessorSummary(
    @JsonProperty("totalRequests")
    long totalRequests,
    
    @JsonProperty("totalAmount")
    BigDecimal totalAmount
) {}