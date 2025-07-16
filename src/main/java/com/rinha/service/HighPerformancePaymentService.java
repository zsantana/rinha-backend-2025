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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class HighPerformancePaymentService {

    private static final Logger log = LoggerFactory.getLogger(HighPerformancePaymentService.class);

    @ConfigProperty(name = "payment.default.url")
    String defaultPaymentUrl;

    @ConfigProperty(name = "payment.fallback.url")
    String fallbackPaymentUrl;

    @ConfigProperty(name = "payment.timeout.millis")
    int timeoutMillis;

    @ConfigProperty(name = "payment.connect.timeout.millis")
    int connectTimeoutMillis;

    private final HttpClient httpClient;
    private final Executor virtualThreadExecutor;

    @Inject
    PaymentoProdutorEventBusRedis produtorEventBusRedis;

    @Inject
    RedisService redisService;

    @Inject
    public HighPerformancePaymentService() {

        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Inicializa o HttpClient usando o Executor de Virtual Threads
        this.httpClient = HttpClient.newBuilder()
                .executor(this.virtualThreadExecutor) // Configura o executor
                .connectTimeout(Duration.ofSeconds(10)) // Exemplo de configuração de timeout
                .build();

        // // HttpClient otimizado para alta performance
        // this.httpClient = HttpClient.newBuilder()
        //         .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
        //         .executor(virtualThreadExecutor)
        //         .version(HttpClient.Version.HTTP_2) // HTTP/2 para melhor performance
        //         .build();
    }

    // Versão assíncrona otimizada
    public CompletableFuture<PaymentResponse> processPaymentAsync(PaymentRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            
            // String building otimizada para alta performance
            String body = buildJsonBody(request);
            
            HttpRequest defaultRequest = HttpRequest.newBuilder()
                    .uri(URI.create(defaultPaymentUrl + "/payments"))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(defaultRequest, 
                        HttpResponse.BodyHandlers.ofString());

                log.info("Payment processed with DEFAULT processor: {}", response.body());
                
                if (isSuccessResponse(response.statusCode())) {
                    PaymentRecord paymentRecord = new PaymentRecord(
                        UUID.randomUUID(),
                        request.amount(),
                        Instant.now(),
                        PaymentRecord.ProcessorType.DEFAULT
                    );
                    // produtorEventBusRedis.publishLocal(paymentRecord);
                    redisService.savePayment(paymentRecord); // Salva no Redis
                    return new PaymentResponse("SUCCESS", "DEFAULT");
                } else {
                    // Fallback direto sem overhead de anotações
                    return null; //processPaymentFallback(request);
                }
            } catch (Exception e) {
                // Log mínimo para não impactar performance
                if (log.isDebugEnabled()) {
                    log.error("Default payment failed: {}", e.getMessage());
                }
                return null;
            }
        }, virtualThreadExecutor);
    }

    // Versão síncrona otimizada
    public PaymentResponse processPayment(PaymentRequest request) {
        
        String body = buildJsonBody(request);
        
        HttpRequest defaultRequest = HttpRequest.newBuilder()
                .uri(URI.create(defaultPaymentUrl + "/payments"))
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(defaultRequest, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (isSuccessResponse(response.statusCode())) {
                // log.info("Payment processed successfully with DEFAULT processor: {}", response.body());
                PaymentRecord paymentRecord = new PaymentRecord(
                        UUID.randomUUID(),
                        request.amount(),
                        Instant.now(),
                        PaymentRecord.ProcessorType.DEFAULT
                    );

                log.info("Payment processed with DEFAULT processor: {}", response.body());
                redisService.savePayment(paymentRecord); // Salva no Redis

                // produtorEventBusRedis.publishLocal(paymentRecord);

                return new PaymentResponse("SUCCESS", "DEFAULT");
            } else {
                return processPaymentFallback(request);
            }
        } catch (Exception e) {
            return processPaymentFallback(request);
        }
    }

    private PaymentResponse processPaymentFallback(PaymentRequest request) {
        
         String body = buildJsonBody(request);

        int maxRetries = 3;
        long baseBackoffMillis = 100; // tempo inicial entre tentativas

        for (int attempt = 1; attempt <= maxRetries; attempt++) {

            HttpRequest fallbackRequest = HttpRequest.newBuilder()
                    .uri(URI.create(fallbackPaymentUrl + "/payments"))
                    .timeout(Duration.ofMillis(baseBackoffMillis))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(fallbackRequest,
                        HttpResponse.BodyHandlers.ofString());

                if (isSuccessResponse(response.statusCode())) {
                    PaymentRecord paymentRecord = new PaymentRecord(
                        UUID.randomUUID(),
                        request.amount(),
                        Instant.now(),
                        PaymentRecord.ProcessorType.FALLBACK
                    );
                    produtorEventBusRedis.publishLocal(paymentRecord);
                    return new PaymentResponse("SUCCESS", "FALLBACK");
                } else {

                    log.warn("Tentativa {} de {} falhou com status {}: {}, {}", 
                            attempt, maxRetries, response.statusCode(), response.body(), body);
                    // Se for erro de cliente (4xx), não faz sentido continuar tentando
                    if (response.statusCode() >= 400 && response.statusCode() < 500) {
                        
                        throw new PaymentServiceException("Fallback service failed", response.statusCode());
                    }
                }
            } catch (PaymentServiceException e) {
                throw e; // Propaga exceções customizadas que já mapeiam status
            } catch (Exception e) {
                log.warn("Erro na tentativa {} de {}: {}", attempt, maxRetries, e.getMessage());
            }

            // Aguarda antes de tentar novamente
            if (attempt < maxRetries) {
                try {
                    long waitMillis = baseBackoffMillis * attempt;
                    Thread.sleep(waitMillis); // backoff exponencial simples
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        throw new PaymentServiceException("Fallback service error after retries", 502);
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

    // Método para batch processing - alta performance
    public CompletableFuture<PaymentResponse[]> processPaymentBatch(PaymentRequest[] requests) {
        CompletableFuture<PaymentResponse>[] futures = new CompletableFuture[requests.length];
        
        for (int i = 0; i < requests.length; i++) {
            futures[i] = processPaymentAsync(requests[i]);
        }
        
        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    PaymentResponse[] results = new PaymentResponse[requests.length];
                    for (int i = 0; i < futures.length; i++) {
                        results[i] = futures[i].join();
                    }
                    return results;
                });
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