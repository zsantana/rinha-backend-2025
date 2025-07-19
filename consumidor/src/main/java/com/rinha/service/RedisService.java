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

    private ValueCommands<String, String> valueCommands;

    @Inject
    RedisDataSource redisDS;

    @PostConstruct
    void init() {
        valueCommands = redisDS.value(String.class);
    }

    @VirtualThreads
    public void savePayment(PaymentRecord paymentRecord) {
        String key = generateKey(paymentRecord.correlationId());
        try {
            String paymentJson = Json.encode(paymentRecord);
            valueCommands.set(key, paymentJson);
            // log.info("✅ Payment salvo no Redis: {}", key);
        } catch (Exception e) {
            log.error("❌ Erro ao salvar payment no Redis - Key: {}, payload {}, exception", key, paymentRecord, e);
            throw new RuntimeException(e);
        }
    }

    private String generateKey(UUID correlationId) {
        return PAYMENT_KEY_PREFIX + ":" + correlationId.toString();
    }
}
