package com.rinha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentProcessorResponse(
    @JsonProperty("message")
    String message
) {}