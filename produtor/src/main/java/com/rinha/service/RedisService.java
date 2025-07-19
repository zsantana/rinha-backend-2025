package com.rinha.service;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRecord;
import com.rinha.dto.PaymentsSummary;
import com.rinha.dto.ProcessorSummary;

@ApplicationScoped
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);
    private static final String PAYMENT_KEY_PREFIX = "payment";

    @Inject
    ReactiveRedisDataSource reactiveRedisDataSource;

    @PostConstruct
    void checkInjection() {
        log.info("🔌 Conectando no Redis com: {}", System.getenv("QUARKUS_REDIS_HOSTS"));
        if (reactiveRedisDataSource == null) {
            log.error("❌ reactiveRedisDataSource não foi injetado!");
        } else {
            log.info("✅ reactiveRedisDataSource injetado com sucesso.");
        }
    }

     public Uni<PaymentsSummary> getPaymentsSummary(Instant from, Instant to) {

        ReactiveKeyCommands<String> keyCommands = reactiveRedisDataSource.key();
        String searchPattern = PAYMENT_KEY_PREFIX + ":*";
        
        log.info("🔍 Buscando pagamentos entre {} e {}", from, to);
    
        return keyCommands.keys(searchPattern)
            .onItem().invoke(keys -> log.info("🔑 Chaves encontradas: {}", keys.size()))
            .onItem().<String>transformToMulti(keys -> 
                Multi.createFrom().iterable(keys)
                    .onItem().transformToUni(key -> 
                        reactiveRedisDataSource.value(String.class).get(key)
                    )
                    .merge(8)
            )
            .onItem().transform(json -> {
                try {
                    return Json.decodeValue(json, PaymentRecord.class);
                } catch (Exception e) {
                    log.error("❌ Erro ao deserializar: {}", json, e);
                    return null;
                }
            })
            .filter(record -> record != null)
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
}