package com.rinha.service;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.virtual.threads.VirtualThreads;
import io.vertx.core.json.Json;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRecord;


@ApplicationScoped
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);
    private static final String PAYMENT_KEY_PREFIX = "payment";
    
    // Cache para strings de chaves frequentemente utilizadas
    private static final ThreadLocal<StringBuilder> KEY_BUILDER = 
        ThreadLocal.withInitial(() -> new StringBuilder(64));

    private ValueCommands<String, String> valueCommands;

    @Inject
    RedisDataSource redisDS;

    @PostConstruct
    void init() {
        valueCommands = redisDS.value(String.class);
    }

    @VirtualThreads
    public void savePayment(PaymentRecord paymentRecord) {
        String key = generateKeyOptimized(paymentRecord.correlationId());
        try {
            // Usa Json.encode que é otimizado para Vert.x
            String paymentJson = Json.encode(paymentRecord);
            
            // Operação assíncrona mas não aguarda resultado para máxima performance
            valueCommands.set(key, paymentJson);
            log.info("✅ Payment salvo no Redis: {}", key);
            
        } catch (Exception e) {
            log.error("❌ Erro ao salvar payment no Redis - Key: {}, exception: {}", key, e.getMessage());
            // Não propaga a exceção para não quebrar o fluxo principal
            // Em cenários de alta performance, pode ser melhor logar e continuar
        }
    }

    private String generateKeyOptimized(UUID correlationId) {
        StringBuilder sb = KEY_BUILDER.get();
        sb.setLength(0); // Reset buffer
        sb.append(PAYMENT_KEY_PREFIX).append(':').append(correlationId);
        return sb.toString();
    }
}