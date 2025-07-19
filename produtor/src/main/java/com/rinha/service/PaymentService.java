package com.rinha.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRecord;
import com.rinha.dto.PaymentRequest;
import com.rinha.exception.PaymentServiceException;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    @ConfigProperty(name = "payment.connect.timeout.millis")
    long connectTimeoutMillis;

    private final HttpClient httpClient;
    private final Executor virtualThreadExecutor;

    @Inject
    RedisServiceImperative redisService;


    public PaymentService() {

        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Inicializa o HttpClient usando o Executor de Virtual Threads
        this.httpClient = HttpClient.newBuilder()
                .executor(this.virtualThreadExecutor) 
                //  .connectTimeout(Duration.ofMillis(2000))
                .build();

    }


    public void processPaymentDefault(PaymentRequest request)  {
        
        CompletableFuture.runAsync(() -> {
            try {
                
                processWithHttpClient(request, defaultPaymentUrl, PaymentRecord.ProcessorType.DEFAULT);

                PaymentRecord record = new PaymentRecord(
                    request.correlationId(),
                    request.amount(),
                    Instant.now(),
                    PaymentRecord.ProcessorType.DEFAULT
                    );

                redisService.savePayment(record);
                log.info("✅ Pagamento processado com sucesso: {}", request.correlationId());

            } catch (PaymentServiceException e) {
                log.error("❌ PaymentServiceException: {}", e.getMessage());
               throw new RuntimeException(e); 
            } catch (Exception e) {
               log.error("❌ Default payment failed: {}", e.getMessage());
                processPaymentFallback(request);
            }
        }, virtualThreadExecutor);

    }

    private void processPaymentFallback(PaymentRequest request)  {

       try {

            processWithHttpClient(request, fallbackPaymentUrl, PaymentRecord.ProcessorType.FALLBACK);

            PaymentRecord record = new PaymentRecord(
                    request.correlationId(),
                    request.amount(),
                    Instant.now(),
                    PaymentRecord.ProcessorType.FALLBACK
                    );

            redisService.savePayment(record);
            log.info("✅ Pagamento processado com sucesso no fallback: {}", request.correlationId());

        } catch (Exception e) {
            log.error("❌ Erro no processPaymentFallback: {}", e.getMessage());
            throw new PaymentServiceException("processPaymentFallback service error", 502);
        }
        
    }

    
    private void processWithHttpClient(PaymentRequest request, String baseUrl, 
                                              PaymentRecord.ProcessorType processorType) throws Exception  {

        var requestedAt = Instant.now().atZone(ZoneOffset.UTC).toInstant();
        var body = buildJsonBody(request, requestedAt);

        var httpRequest = buildHttpRequest(baseUrl, body, processorType == PaymentRecord.ProcessorType.FALLBACK);
        httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
    }

    private HttpRequest buildHttpRequest(String baseUrl, String body, boolean isFallback) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/payments"))
                .timeout(Duration.ofMillis(1000))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }


    private String buildJsonBody(PaymentRequest request, Instant requestedAt) {

        return "{" +
                "\"correlationId\":\"" + request.correlationId() + "\"," +
                "\"amount\":" + request.amount() + "," +
                "\"requestedAt\":\"" + DateTimeFormatter.ISO_INSTANT.format(requestedAt) + "\"" +
                "}";
    }

    // Health check otimizado
    public boolean isHealthy() {

        try {

            var healthRequest = HttpRequest.newBuilder()
                    .uri(URI.create(defaultPaymentUrl + "/payments/service-health"))
                    .timeout(Duration.ofMillis(1000))
                    .GET()
                    .build();
            
            var response = httpClient.send(healthRequest, HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }
}