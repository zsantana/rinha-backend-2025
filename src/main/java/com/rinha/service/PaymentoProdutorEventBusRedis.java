package com.rinha.service;

import com.rinha.dto.PaymentRecord;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentoProdutorEventBusRedis {
    
    @Inject
    io.vertx.mutiny.core.eventbus.EventBus eventBus;

    public void publishLocal(PaymentRecord request) {
         System.out.println("📤 Enviando pagamento para o produtor PaymentoProdutorEventBusRedis: " + request);
        eventBus.publish("redis", JsonObject.mapFrom(request));

    }

}
