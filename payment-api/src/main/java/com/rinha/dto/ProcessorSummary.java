package com.rinha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProcessorSummary(
    @JsonProperty("totalRequests")
    long totalRequests,
    
    @JsonProperty("totalAmount")
    BigDecimal totalAmount
) {}