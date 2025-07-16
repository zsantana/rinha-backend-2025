package com.rinha.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;

import com.rinha.dto.PaymentRecord;
import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentsSummary;
import com.rinha.exception.PaymentServiceException;

@ApplicationScoped
public class PaymentService {

    // private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Inject
    RedisService redisService;

    @Inject
    HighPerformancePaymentService highPerformancePaymentService;

    public Uni<Void> savePayment(PaymentRequest request) {
        var result = highPerformancePaymentService.processPaymentAsync(request);
        // return redisService.savePayment(request);
        return Uni.createFrom().voidItem();
    }

    public Uni<PaymentsSummary> getPaymentsSummary(Instant from, Instant to) {
        return redisService.getPaymentsSummary(from, to);
    }
}