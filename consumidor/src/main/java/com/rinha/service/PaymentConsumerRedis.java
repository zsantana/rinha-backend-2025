package com.rinha.service;

import com.rinha.dto.PaymentRequest;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands.RedisSubscriber;

import io.quarkus.runtime.Startup;
import io.quarkus.virtual.threads.VirtualThreads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@ApplicationScoped
@Startup // Garante que seja iniciado junto com o app
public class PaymentConsumerRedis implements Consumer<PaymentRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentConsumerRedis.class);

    private final RedisSubscriber subscriber;

    @Inject
    private PaymentService paymentService;

    public PaymentConsumerRedis(RedisDataSource ds, PaymentService paymentService) {
        this.paymentService = paymentService;

        // Cria cliente Pub/Sub tipado
        PubSubCommands<PaymentRequest> pub = ds.pubsub(PaymentRequest.class);

        // Inscreve no canal 'payments' e define this::accept como handler
        this.subscriber = pub.subscribe("payments", this);
        LOG.info("✅ Inscrito no canal Redis 'payments'");
    }

    @Override
    @VirtualThreads
    public void accept(PaymentRequest notification) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("📥 Mensagem recebida do Redis: {}", notification);
            }
            
            // Processamento assíncrono para não bloquear o subscriber
            paymentService.processPaymentDefault(notification)
                .exceptionally(throwable -> {
                    LOG.error("❌ Erro ao processar mensagem Redis de forma assíncrona: {}", throwable.getMessage());
                    return null;
                });
                
        } catch (Exception e) {
            LOG.error("❌ Erro crítico ao processar mensagem Redis: {}", e.getMessage());
            // Não propaga exceção para não quebrar o subscriber
        }
    }

    @PreDestroy
    public void terminate() {
        subscriber.unsubscribe(); // Remove inscrição ao encerrar app
        LOG.info("🔌 Unsubscribed do canal Redis 'payments'");
    }
}
