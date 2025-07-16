package com.rinha.service;

import com.rinha.dto.PaymentRequest;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentoProdutorEventBus {
    
    @Inject
    io.vertx.mutiny.core.eventbus.EventBus eventBus;

    public void publishLocal(PaymentRequest request) {
        //Log
        System.out.println("📤 Enviando pagamento para o produtor: " + request);
        eventBus.publish("payments", JsonObject.mapFrom(request));

    }

}
