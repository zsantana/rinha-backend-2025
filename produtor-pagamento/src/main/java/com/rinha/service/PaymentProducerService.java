package com.rinha.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentProducerService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentProducerService.class);

    private static final String CHANNEL = "payments";

    @Inject
    ReactiveRedisDataSource redis;

    public Uni<Void> publish(PaymentRequest request) {

        return redis
                .pubsub(PaymentRequest.class)
                .publish(CHANNEL, request)
                .invoke(count -> LOG.info("📤 Mensagem publicada no REDIS"))
                .onFailure()
                    .retry().withBackOff(Duration.ofMillis(100), Duration.ofMillis(200))
                    .atMost(3)
                .replaceWithVoid();
    }

}
