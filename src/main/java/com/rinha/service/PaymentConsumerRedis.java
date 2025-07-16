package com.rinha.service;

import com.rinha.dto.PaymentRecord;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentConsumerRedis {

    @Inject
    io.vertx.mutiny.core.eventbus.EventBus eventBus;

    @Inject
    RedisService redisService;

    // public void init(@Observes StartupEvent ev) {
    //     System.out.println("📥 Pagamento recebido via EventBus: ");
    //     eventBus.<JsonObject>consumer("redis", message -> {
    //         PaymentRecord request = message.body().mapTo(PaymentRecord.class);
    //         redisService.savePayment(request);
            
    //     });
    // }
}

