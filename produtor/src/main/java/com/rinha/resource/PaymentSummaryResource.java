package com.rinha.resource;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentsSummary;
import com.rinha.service.RedisService;
import com.rinha.util.DateTimeUtils;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("")
public class PaymentSummaryResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentSummaryResource.class);

    @Inject
    RedisService redisService;

    @GET
    @Path("/payments-summary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<PaymentsSummary> getPaymentsSummary(
            @QueryParam("from") String inicio,
            @QueryParam("to") String fim) {

        LOG.info("### getPaymentsSummary: {}, {} (Reactive)", inicio, fim);

        Instant from = DateTimeUtils.parseToInstantNullable(inicio);
        Instant to = DateTimeUtils.parseToInstantNullable(fim);

        return redisService.getPaymentsSummary(from, to)
                .onItem().invoke(summary -> LOG.info("### Resumo de pagamentos: {}", summary))
                .onFailure().invoke(e -> LOG.error("### Erro ao obter resumo de pagamentos: {}", e.getMessage()));
    }
    
}
