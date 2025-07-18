package com.rinha.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentResponse(String status, String message) {}
