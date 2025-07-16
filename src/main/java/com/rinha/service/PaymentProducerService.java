package com.rinha.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentProducerService {

    // private static final Logger LOG = LoggerFactory.getLogger(PaymentProducerService.class);

    // private static final String CHANNEL = "payments";

    // @Inject
    // ReactiveRedisDataSource redis;

    // public Uni<Void> publish(PaymentRequest request) {

    //     return redis
    //             .pubsub(PaymentRequest.class)
    //             .publish(CHANNEL, request)
    //             .invoke(count -> LOG.info("📤 Mensagem publicada no REDIS"))
    //             .onFailure()
    //                 .retry().withBackOff(Duration.ofMillis(100), Duration.ofMillis(200))
    //                 .atMost(3)
    //             .replaceWithVoid();
    // }

    // public void publishVT(PaymentRequest request) {
    //     int attempts = 0;
    //     int maxAttempts = 3;
    //     Duration backoff = Duration.ofMillis(100);

    //     while (attempts < maxAttempts) {
    //         try {
    //             redis.pubsub(PaymentRequest.class)
    //                 .publish(CHANNEL, request)
    //                 .await().indefinitely(); // bloqueia até a publicação completar

    //             // LOG.info("📤 Mensagem publicada no REDIS");
    //             return; // sucesso
    //         } catch (Exception e) {
    //             attempts++;
    //             LOG.warn("Tentativa {} de publicação falhou: {}", attempts, e.getMessage());

    //             if (attempts >= maxAttempts) {
    //                 LOG.error("❌ Falha ao publicar mensagem no Redis após {} tentativas", attempts);
    //                 throw new RuntimeException("Falha ao publicar no Redis", e);
    //             }

    //             try {
    //                 Thread.sleep(backoff.toMillis());
    //                 backoff = backoff.plusMillis(100); // aumenta o backoff
    //             } catch (InterruptedException ie) {
    //                 Thread.currentThread().interrupt();
    //                 throw new RuntimeException("Thread interrompida durante backoff", ie);
    //             }
    //         }
    //     }
// }



}
