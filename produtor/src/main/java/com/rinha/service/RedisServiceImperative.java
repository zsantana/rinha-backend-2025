package com.rinha.service;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.vertx.core.json.Json;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRecord;
import com.rinha.dto.PaymentsSummary;
import com.rinha.dto.ProcessorSummary;

@ApplicationScoped
public class RedisServiceImperative {

    private static final Logger log = LoggerFactory.getLogger(RedisServiceImperative.class);
    private static final String PAYMENT_KEY_PREFIX = "payment";

    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;

    @Inject
    RedisDataSource redisDS;

    @PostConstruct
    void init() {
        valueCommands = redisDS.value(String.class);
        keyCommands = redisDS.key();
    }

    public void savePayment(PaymentRecord paymentRecord) {
        String key = generateKey(paymentRecord.correlationId());
        try {
            String paymentJson = Json.encode(paymentRecord);
            valueCommands.set(key, paymentJson);
            // log.info("✅ Payment salvo no Redis: {}", key);
        } catch (Exception e) {
            log.error("❌ Erro ao salvar payment no Redis - Key: {}", key, e);
            throw new RuntimeException(e);
        }
    }


    public PaymentsSummary getPaymentsSummary(Instant from, Instant to) {

        String searchPattern = PAYMENT_KEY_PREFIX + ":*";
        List<String> keys = keyCommands.keys(searchPattern);

        log.info("🔑 Chaves encontradas: {}", keys.size());

        List<PaymentRecord> records = keys.stream()
                .map(key -> {
                    try {
                        String json = valueCommands.get(key);
                        if (json != null) {
                            return Json.decodeValue(json, PaymentRecord.class);
                        }
                    } catch (Exception e) {
                        log.error("❌ Erro ao deserializar valor da chave {}", key, e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(record -> !record.requestedAt().isBefore(from) && !record.requestedAt().isAfter(to))
                .collect(Collectors.toList());

        return calculateSummaries(records);
    }

    private PaymentsSummary calculateSummaries(List<PaymentRecord> records) {
        Map<PaymentRecord.ProcessorType, ProcessorSummary> summaries = new HashMap<>();
        summaries.put(PaymentRecord.ProcessorType.DEFAULT, new ProcessorSummary(0, BigDecimal.ZERO));
        summaries.put(PaymentRecord.ProcessorType.FALLBACK, new ProcessorSummary(0, BigDecimal.ZERO));

        for (PaymentRecord record : records) {
            ProcessorSummary currentSummary = summaries.get(record.processorType());
            long updatedTotalRequests = currentSummary.totalRequests() + 1;
            BigDecimal updatedTotalAmount = currentSummary.totalAmount().add(record.amount());
            summaries.put(record.processorType(), new ProcessorSummary(updatedTotalRequests, updatedTotalAmount));
        }

        return new PaymentsSummary(
            summaries.get(PaymentRecord.ProcessorType.DEFAULT),
            summaries.get(PaymentRecord.ProcessorType.FALLBACK)
        );
    }

    private String generateKey(UUID correlationId) {
        return PAYMENT_KEY_PREFIX + ":" + correlationId.toString();
    }
}
