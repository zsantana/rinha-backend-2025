package com.rinha.service;

import com.rinha.dto.PaymentRequest;

import io.quarkus.virtual.threads.VirtualThreads;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
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
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(1000);

    @Inject
    RedisAPI redisAPI;

    public void publish(PaymentRequest request) {
        JsonObject json = JsonObject.mapFrom(request);
        LOG.info("📤 Publicando pagamento no Redis: {}", json.encode());

        publishWithRetry(json.encode(), request, 0);
    }

    private void publishWithRetry(String message, PaymentRequest originalRequest, int attemptNumber) {
        redisAPI.publish("payments", message)
            .ifNoItem()
                .after(TIMEOUT)
                .failWith(() -> new RuntimeException("Timeout ao publicar no Redis após " + TIMEOUT.getSeconds() + " segundos"))
            .subscribe().with(
                result -> {
                    LOG.info("✅ Mensagem publicada no Redis com sucesso na tentativa {}: {}", 
                            attemptNumber + 1, originalRequest);
                },
                failure -> handleFailure(failure, message, originalRequest, attemptNumber)
            );
    }

    private void handleFailure(Throwable failure, String message, PaymentRequest originalRequest, int attemptNumber) {
        LOG.warn("⚠️ Falha na tentativa {} ao publicar no Redis: {}", 
                attemptNumber + 1, failure.getMessage());

        if (attemptNumber < MAX_RETRIES - 1) {
            // Ainda temos tentativas restantes
            LOG.info("🔄 Tentando novamente em {}ms (tentativa {}/{})", 
                    RETRY_DELAY.toMillis(), attemptNumber + 2, MAX_RETRIES);
            
            // Agendando retry com delay
            Vertx.currentContext().owner().setTimer(RETRY_DELAY.toMillis(), timerId -> {
                publishWithRetry(message, originalRequest, attemptNumber + 1);
            });
        } else {
            // Esgotamos todas as tentativas
            LOG.error("❌ Falha definitiva ao publicar no Redis após {} tentativas. Último erro: {}", 
                    MAX_RETRIES, failure.getMessage());
            
            // Aqui você pode implementar estratégias como:
            // - Enviar para uma dead letter queue
            // - Persistir em banco para reprocessamento posterior
            // - Notificar sistema de monitoramento
            handleFinalFailure(originalRequest, failure);
        }
    }

    private void handleFinalFailure(PaymentRequest request, Throwable failure) {
        // Implementar estratégia de fallback
        // Exemplos:
        // 1. Salvar em banco para reprocessamento
        // 2. Enviar para dead letter queue
        // 3. Notificar sistema de alertas
        
        LOG.error("🚨 Processamento de fallback necessário para pagamento: {}", request);
        
        // Exemplo: você pode injetar um serviço de fallback aqui
        // fallbackService.handleFailedPayment(request, failure);
    }

    // Método alternativo usando Uni diretamente para melhor controle
    @VirtualThreads
    public void publishAsync(PaymentRequest request) {
        JsonObject json = JsonObject.mapFrom(request);
        LOG.info("📤 Publicando pagamento no Redis (async): {}", json.encode());

        redisAPI.publish("payments", json.encode())
            .ifNoItem()
                .after(TIMEOUT)
                .failWith(() -> new RuntimeException("Timeout ao publicar no Redis"))
            .onFailure()
                .retry()
                .withBackOff(RETRY_DELAY, Duration.ofSeconds(10))
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
}