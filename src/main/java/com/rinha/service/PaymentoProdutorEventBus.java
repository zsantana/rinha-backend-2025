package com.rinha.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentoProdutorEventBus {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentConsumerEventBus.class);
    
    @Inject
    io.vertx.mutiny.core.eventbus.EventBus eventBus;

    public void publishLocal(PaymentRequest request) {
        LOG.info("📤 Enviando pagamento para o produtor: {}" , request);
        eventBus.publish("payments", JsonObject.mapFrom(request));
    }

}
