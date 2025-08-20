package com.rinha.service;

import jakarta.annotation.PostConstruct;
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

    @ConfigProperty(name = "quarkus.http.io-threads", defaultValue = "16")
    int ioThreads;

    private final HttpClient httpClient;
    private final Executor virtualThreadExecutor;
    
    // Cache para URIs frequentemente utilizados
    private final URI defaultPaymentUri;
    private final URI fallbackPaymentUri;
    
    // StringBuilder pool para JSON building (thread-local para performance)
    private static final ThreadLocal<StringBuilder> JSON_BUILDER = 
        ThreadLocal.withInitial(() -> new StringBuilder(256));
    
    // DateTimeFormatter cache (thread-safe)
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Inject
    RedisService redisService;

    public PaymentService(@ConfigProperty(name = "payment.default.url") String defaultPaymentUrl,
                         @ConfigProperty(name = "payment.fallback.url") String fallbackPaymentUrl,
                         @ConfigProperty(name = "payment.connect.timeout.millis") long connectTimeoutMillis) {

        // Validação inicial dos timeouts para evitar PT0S
        this.connectTimeoutMillis = Math.max(connectTimeoutMillis, 2000); // Mínimo 2s
        
        this.defaultPaymentUri = URI.create(defaultPaymentUrl + "/payments");
        this.fallbackPaymentUri = URI.create(fallbackPaymentUrl + "/payments");
        
        // OTIMIZAÇÃO: Virtual Thread Executor otimizado
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // OTIMIZAÇÃO: HttpClient com Virtual Threads e configurações agressivas
        this.httpClient = HttpClient.newBuilder()
            .executor(this.virtualThreadExecutor)
            .connectTimeout(Duration.ofMillis(this.connectTimeoutMillis))
            .version(HttpClient.Version.HTTP_2) // HTTP/2 para multiplexing
            .build();
    }

    @PostConstruct
    public void validateConfiguration() {
        log.info("🔧 Validando configurações do PaymentService...");
        
        if (connectTimeoutMillis < 1000) {
            log.warn("⚠️ Timeout muito baixo ({}ms), usando 2000ms", connectTimeoutMillis);
            connectTimeoutMillis = 2000;
        }
        
        log.info("✅ PaymentService configurado - Timeout: {}ms, VirtualThreads: enabled", 
                connectTimeoutMillis);
        log.info("📍 URIs: Default={}, Fallback={}", defaultPaymentUri, fallbackPaymentUri);
    }

    /**
     * OTIMIZAÇÃO: Processa pagamento usando Virtual Threads para máxima performance
     */
    public CompletableFuture<Void> processPaymentDefault(PaymentRequest request) {
        // Virtual Thread com CompletableFuture para non-blocking
        return CompletableFuture.runAsync(() -> {
            try {
                // Processa com timeout agressivo
                processWithHttpClientOptimized(request, defaultPaymentUri, PaymentRecord.ProcessorType.DEFAULT);
                
                // Salva no Redis de forma assíncrona
                savePaymentRecordAsync(request, PaymentRecord.ProcessorType.DEFAULT);
                log.info("✅ Payment processed: {}", request.correlationId());

            } catch (PaymentServiceException e) {
                log.error("❌ PaymentServiceException: {}", e.getMessage());
                throw new RuntimeException(e); 
            } catch (Exception e) {
                log.warn("⚠️ Default payment failed, trying fallback: {}", e.getMessage());
                // Fallback assíncrono sem bloquear
                processPaymentFallbackAsync(request);
            }
        }, virtualThreadExecutor);
    }

    /**
     * OTIMIZAÇÃO: Processamento fallback assíncrono
     */
    private CompletableFuture<Void> processPaymentFallbackAsync(PaymentRequest request) {
        return CompletableFuture.runAsync(() -> {
            try {
                processWithHttpClientOptimized(request, fallbackPaymentUri, PaymentRecord.ProcessorType.FALLBACK);
                savePaymentRecordAsync(request, PaymentRecord.ProcessorType.FALLBACK);
                
                log.info("✅ Fallback payment success: {}", request.correlationId());
            } catch (Exception e) {
                log.error("❌ Fallback payment failed: {}", e.getMessage());
                throw new PaymentServiceException("Fallback service error", 502);
            }
        }, virtualThreadExecutor);
    }

    /**
     * OTIMIZAÇÃO: HTTP Client com Virtual Threads e timeouts otimizados
     */
    private void processWithHttpClientOptimized(PaymentRequest request, URI paymentUri, 
                                              PaymentRecord.ProcessorType processorType) throws Exception {

        var requestedAt = Instant.now();
        var jsonBody = buildJsonBodyOptimized(request, requestedAt);
        var httpRequest = buildHttpRequestOptimized(paymentUri, jsonBody);
        
        // OTIMIZAÇÃO: Send assíncrono com timeout
        var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        
        // Validação básica da resposta
        if (response.statusCode() >= 400) {
            throw new PaymentServiceException("HTTP error: " + response.statusCode(), response.statusCode());
        }
        
        // Log apenas se necessário para debug
        if (log.isDebugEnabled()) {
            log.debug("Payment request sent - Status: {}", response.statusCode());
        }
    }

    /**
     * OTIMIZAÇÃO: Salva record no Redis de forma assíncrona
     */
    private void savePaymentRecordAsync(PaymentRequest request, PaymentRecord.ProcessorType processorType) {
        // Fire-and-forget para não bloquear o processamento principal
        CompletableFuture.runAsync(() -> {
            try {
                PaymentRecord record = new PaymentRecord(
                    request.correlationId(),
                    request.amount(),
                    Instant.now(),
                    processorType
                );
                redisService.savePayment(record);
            } catch (Exception e) {
                // Log mas não propaga para não afetar o processamento principal
                log.warn("⚠️ Redis save failed: {}", e.getMessage());
            }
        }, virtualThreadExecutor);
    }

    private void processPaymentFallback(PaymentRequest request) {
        try {
            processWithHttpClientOptimized(request, fallbackPaymentUri, PaymentRecord.ProcessorType.FALLBACK);
            savePaymentRecordAsync(request, PaymentRecord.ProcessorType.FALLBACK);
            log.info("✅ Pagamento processado com sucesso no fallback: {}", request.correlationId());

        } catch (Exception e) {
            log.error("❌ Erro no processPaymentFallback: {}", e.getMessage());
            throw new PaymentServiceException("processPaymentFallback service error", 502);
        }
    }

    private HttpRequest buildHttpRequestOptimized(URI paymentUri, String body) {
        // Garante que o timeout seja pelo menos 1 segundo para evitar PT0S
        long timeoutMillis = Math.max(connectTimeoutMillis, 1000);
        
        return HttpRequest.newBuilder()
                .uri(paymentUri)
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private String buildJsonBodyOptimized(PaymentRequest request, Instant requestedAt) {
        StringBuilder sb = JSON_BUILDER.get();
        sb.setLength(0); // Reset buffer
        
        return sb.append("{\"correlationId\":\"")
                .append(request.correlationId())
                .append("\",\"amount\":")
                .append(request.amount())
                .append(",\"requestedAt\":\"")
                .append(ISO_FORMATTER.format(requestedAt))
                .append("\"}")
                .toString();
    }
    
}