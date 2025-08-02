package com.rinha.service;

import com.rinha.dto.PaymentRequest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.RedisAPI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;


@ApplicationScoped
public class PaymentProducerRedis {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentProducerRedis.class);
    
    // Configurações de timeout e retry
    private static final Duration TIMEOUT = Duration.ofMillis(100);
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(100);

    @Inject
    RedisAPI redisAPI;

    public void publishAsync(PaymentRequest request) {

        JsonObject json = JsonObject.mapFrom(request);
        LOG.info("📤 Publicando pagamento no Redis (async): {}", json.encode());

        redisAPI.publish("payments", json.encode())
            .ifNoItem()
                .after(TIMEOUT)
                .failWith(() -> new RuntimeException("Timeout ao publicar no Redis"))
            .onFailure()
                .retry()
                .withBackOff(RETRY_DELAY, Duration.ofMillis(4000))
                .atMost(MAX_RETRIES)
            .onItem().invoke(result -> 
                LOG.info("✅ Mensagem publicada no Redis com sucesso (async): {}", request))
            .onFailure().invoke(failure -> 
                LOG.error("❌ Falha definitiva ao publicar no Redis (async): {}, payload {}", failure.getMessage(), request))
            .subscribe().with(
                success -> LOG.info("Sucesso"),
                failure -> LOG.error("Erro: {}", failure.getMessage())
            )    
            ;

    }


    public Uni<Void> publishAsync2(PaymentRequest request) {
        JsonObject json = JsonObject.mapFrom(request);
        LOG.info("📤 Publicando pagamento no Redis (async): {}", json.encode());

        redisAPI.publish("payments", json.encode())
            .ifNoItem()
                .after(TIMEOUT)
                .failWith(() -> new RuntimeException("⏱️ Timeout ao publicar no Redis"))
            .onFailure()
                .retry()
                .withBackOff(RETRY_DELAY, Duration.ofMillis(600))
                .atMost(MAX_RETRIES)
            .onItem().invoke(result -> 
                LOG.info("✅ Mensagem publicada no Redis com sucesso (async): {}", request))
            .onFailure().invoke(failure -> 
                LOG.error("❌ Falha definitiva ao publicar no Redis (async): {}, payload: {}", 
                        failure.getMessage(), request))
            .subscribe().with(
                success -> LOG.info("Sucesso"),
                failure -> LOG.error("Erro: {}", failure.getMessage())
            )   
            ;

           return  Uni.createFrom().voidItem();
    }


    
}