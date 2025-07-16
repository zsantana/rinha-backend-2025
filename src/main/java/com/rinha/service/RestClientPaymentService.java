package com.rinha.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.http.*;
import java.net.URI;
import java.time.Duration;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentResponse;

import java.io.IOException;

@ApplicationScoped
public class RestClientPaymentService {

    @ConfigProperty(name = "payment.default.url", defaultValue = "http://localhost:8081")
    String defaultPaymentUrl;

    @ConfigProperty(name = "payment.fallback.url", defaultValue = "http://localhost:8082")
    String fallbackPaymentUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .executor(Runnable::run)
            .build();

    public PaymentResponse processPayment(PaymentRequest request) {

        String correlationId = request.correlationId().toString();
        String amount = request.amount().toString();

        var body = String.format("{\"correlationId\":\"%s\",\"amount\":%s}",
                                          correlationId,
                                          amount);

        var defaultRequest = HttpRequest.newBuilder()
                .uri(URI.create(defaultPaymentUrl))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            var response = httpClient.send(defaultRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new PaymentResponse("SUCCESS", "Processed by DEFAULT");
            } else {
                throw new RuntimeException("Default payment failed with code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            return callFallback(request, body);
        }
    }

    private PaymentResponse callFallback(PaymentRequest request, String body) {

        var fallbackRequest = HttpRequest.newBuilder()
                .uri(URI.create(fallbackPaymentUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            var response = httpClient.send(fallbackRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new PaymentResponse("SUCCESS", "Processed by FALLBACK");
            } else {
                return new PaymentResponse("FAILED", "Fallback failed: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            return new PaymentResponse("FAILED", "Fallback error: " + e.getMessage());
        }
    }
}