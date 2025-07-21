package com.rinha.resource;

import java.time.Instant;

import com.rinha.util.DateTimeUtils;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentsSummary;
import com.rinha.service.PaymentProducerRedis;
import com.rinha.service.RedisService;

import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.virtual.threads.VirtualThreads;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

@Path("")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResource.class);

    @ConfigProperty(name = "payment.default.url")
    String defaultPaymentUrl;

    @Inject
    PaymentProducerRedis paymentProducerRedis;

    @Inject
    RedisService redisService;

    @Inject
    Validator validator;

    
    @POST
    @Path("/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response send2(PaymentRequest request) {

        paymentProducerRedis.publishAsync(request);
        return Response.ok("ok").build();

    }


    @GET
    @Path("/payments-summary")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking 
    public Uni<PaymentsSummary> getPaymentsSummary(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr) {

        LOG.info("### getPaymentsSummary: {}, {}", fromStr, toStr);

        Instant from = DateTimeUtils.parseToInstantNullable(fromStr);
        Instant to = DateTimeUtils.parseToInstantNullable(toStr);

        return redisService.getPaymentsSummary(from, to)
                .onItem().invoke(summary -> LOG.info("### Resumo de pagamentos: {}", summary))
                .onFailure().invoke(e -> LOG.error("### Erro ao obter resumo de pagamentos: {}", e.getMessage()));
    }

}
