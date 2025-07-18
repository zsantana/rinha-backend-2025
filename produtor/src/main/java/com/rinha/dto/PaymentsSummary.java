package com.rinha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentsSummary(
    @JsonProperty("default")
    ProcessorSummary defaultProcessor,
    
    @JsonProperty("fallback")
    ProcessorSummary fallbackProcessor
) {}