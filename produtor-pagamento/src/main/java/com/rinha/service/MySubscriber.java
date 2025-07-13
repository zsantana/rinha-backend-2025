package com.rinha.service;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
// @Startup 
public class MySubscriber implements Consumer<PaymentRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(MySubscriber.class);

    private final PubSubCommands<PaymentRequest> pub;

    public MySubscriber(RedisDataSource ds) {
        pub = ds.pubsub(PaymentRequest.class);
        pub.subscribe("payments", this);
    }

    @Override
    public void accept(PaymentRequest notification) {
        LOG.info("### 📥 Mensagem recebida: {} ", notification);
    }

    // @PreDestroy
    // public void terminate() {
    //     subscriber.unsubscribe(); // Unsubscribe from all subscribed channels
    // }
}
