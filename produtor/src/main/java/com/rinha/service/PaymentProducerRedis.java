package com.rinha.service.redis;

import com.rinha.dto.PaymentRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.RedisAPI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PaymentProducerRedis {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentProducerRedis.class);

    @Inject
    RedisAPI redisAPI;

    public void publish(PaymentRequest request) {

        JsonObject json = JsonObject.mapFrom(request);
        LOG.info("📤 Publicando pagamento no Redis: {}", json.encode());

        redisAPI.publish("payments", json.encode())
            .subscribe().with(
                result -> LOG.info("✅ Mensagem publicada no Redis com sucesso: {}"),
                failure -> LOG.error("❌ Falha ao publicar no Redis: {}", failure.getMessage())
            );
    }
}
