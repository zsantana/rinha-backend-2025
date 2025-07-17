package com.rinha.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRecord;
import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentResponse;
import com.rinha.exception.PaymentServiceException;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @ConfigProperty(name = "payment.default.url")
    String defaultPaymentUrl;

    @ConfigProperty(name = "payment.fallback.url")
    String fallbackPaymentUrl;

    @ConfigProperty(name = "payment.timeout.millis")
    int timeoutMillis;

    @ConfigProperty(name = "payment.connect.timeout.millis")
    long connectTimeoutMillis;

    private final HttpClient httpClient;
    private final Executor virtualThreadExecutor;

    @Inject
    RedisService redisService;

    public PaymentService() {

        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Inicializa o HttpClient usando o Executor de Virtual Threads
        this.httpClient = HttpClient.newBuilder()
                .executor(this.virtualThreadExecutor) // Configura o executor
                .connectTimeout(Duration.ofMillis(1500)) // Exemplo de configuração de timeout
                .build();
    }

    public CompletableFuture<PaymentResponse> processPaymentAsync(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processWithHttpClient(request, defaultPaymentUrl, PaymentRecord.ProcessorType.DEFAULT, true);
            } catch (Exception e) {
                log.error("❌ Default payment failed: {}", e.getMessage());
                return processPaymentFallback(request);
            }
        }, virtualThreadExecutor);
    }

    private PaymentResponse processPaymentFallback(PaymentRequest request) {
        try {
            return processWithHttpClient(
                request,
                fallbackPaymentUrl,
                PaymentRecord.ProcessorType.FALLBACK,
                true
            );
        } catch (PaymentServiceException e) {
            throw e; // já tem status apropriado
        } catch (Exception e) {
            log.error("❌ Erro no fallback: {}", e.getMessage());
            throw new PaymentServiceException("Fallback service error", 502);
        }
    }

    private PaymentResponse processWithHttpClient(PaymentRequest request, String baseUrl, 
                                                PaymentRecord.ProcessorType processorType,
                                                boolean logWarnings) throws Exception {
        String body = buildJsonBody(request);
        HttpRequest httpRequest = buildHttpRequest(baseUrl, body, processorType == PaymentRecord.ProcessorType.FALLBACK);

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (isSuccessResponse(response.statusCode())) {
            PaymentRecord record = new PaymentRecord(
                request.correlationId(),
                request.amount(),
                Instant.now(),
                processorType
            );
            redisService.savePayment(record);
            log.info("Payment processed with {} processor: {}", processorType, response.body());
            return new PaymentResponse("SUCCESS", processorType.name());
        } else {
            if (logWarnings) {
                log.warn("❌ Falha no pagamento ({}): status {}, body: {}", processorType, response.statusCode(), response.body());
            }

            if (response.statusCode() == 422) {
                throw new PaymentServiceException("Unprocessable entity", 422);
            }
            throw new Exception("❌ Erro ao processar pagamento com status " + response.statusCode());
        }
    }

    private HttpRequest buildHttpRequest(String baseUrl, String body, boolean isFallback) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/payments"))
                .timeout(Duration.ofMillis(100))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }



    // String building otimizado - sem StringBuilder para casos simples
    private String buildJsonBody(PaymentRequest request) {
        return "{\"correlationId\":\"" + request.correlationId() + 
               "\",\"amount\":" + request.amount() + "}";
    }

    // Método inline para verificação rápida
    private boolean isSuccessResponse(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }


    // Health check otimizado
    public boolean isHealthy() {
        try {
            HttpRequest healthRequest = HttpRequest.newBuilder()
                    .uri(URI.create(defaultPaymentUrl + "/payments/service-health"))
                    .timeout(Duration.ofMillis(500))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(healthRequest, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}