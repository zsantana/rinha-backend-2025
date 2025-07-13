package com.rinha.service;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rinha.dto.PaymentRecord;
import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentsSummary;
import com.rinha.dto.ProcessorSummary;

@ApplicationScoped
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_KEY_PREFIX = "payment:";

    @Inject
    ReactiveRedisDataSource reactiveRedisDataSource;

    @Inject
    ObjectMapper objectMapper;

    public Uni<Void> savePayment(PaymentRequest request) {

        PaymentRecord paymentRecord = new PaymentRecord(
            UUID.randomUUID(),
            request.amount(),
            Instant.now(),
            PaymentRecord.ProcessorType.DEFAULT
        );

        String key = generateKey(paymentRecord.correlationId());
        return reactiveRedisDataSource
            .value(PaymentRecord.class)
            .set(key, paymentRecord)
            .onFailure()
                    .retry().withBackOff(Duration.ofMillis(100), Duration.ofMillis(500))
                    .atMost(3)
                .replaceWithVoid();

    }

    public Uni<PaymentsSummary> getPaymentsSummary(Instant from, Instant to) {
        ReactiveKeyCommands<String> keyCommands = reactiveRedisDataSource.key();

        // log.info("Buscando pagamentos entre {} e {}", from, to);
    
        return keyCommands.keys("payment:*")
            .onItem().<String>transformToMulti(keys -> 
                Multi.createFrom().iterable(keys)
                    .onItem().transformToUni(
                        key -> reactiveRedisDataSource.value(String.class).get(key)
                    )
                    .merge(8) // 👈 controle de concorrência aqui
            )
            .onItem().transform(json -> Json.decodeValue(json, PaymentRecord.class))
            .filter(record -> !record.requestedAt().isBefore(from) && !record.requestedAt().isAfter(to))
            .collect().asList()
            .onItem().transform(this::calculateSummaries);
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
        return PAYMENT_KEY_PREFIX + UUID.randomUUID().toString() + ":" + correlationId.toString();
    }
}
