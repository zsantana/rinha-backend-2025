package com.rinha.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentConsumer.class);

    @Inject
    io.vertx.mutiny.core.eventbus.EventBus eventBus;

    @Inject
    HighPerformancePaymentService highPerformancePaymentService;

    public void init(@Observes StartupEvent ev) {
        LOG.info("### 📥 Pagamento recebido via EventBus: ");
        eventBus.<JsonObject>consumer("payments", message -> {
            PaymentRequest request = message.body().mapTo(PaymentRequest.class);
            highPerformancePaymentService.processPaymentAsync(request);
            LOG.info("### mensagem processada com sucesso ");
        });
    }
}

