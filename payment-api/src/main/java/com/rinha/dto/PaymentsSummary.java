package com.rinha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentsSummary(
    @JsonProperty("default")
    ProcessorSummary defaultProcessor,
    
    @JsonProperty("fallback")
    ProcessorSummary fallbackProcessor
) {}