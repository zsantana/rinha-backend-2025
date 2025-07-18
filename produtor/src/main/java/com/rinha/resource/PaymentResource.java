package com.rinha.resource;

import java.time.Instant;
import java.util.UUID;

import com.rinha.util.DateTimeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentResponse;
import com.rinha.dto.PaymentsSummary;
import com.rinha.exception.PaymentServiceException;
import com.rinha.service.PaymentProducerRedis;
import com.rinha.service.PaymentService;
import com.rinha.service.RedisService;

import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.smallrye.mutiny.Uni;

@Path("")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResource.class);

    @Inject
    PaymentService paymentService;

    @Inject
    PaymentProducerRedis paymentProducerRedis;

    @Inject
    RedisService redisService;

    @Inject
    Validator validator;

    
    // @POST
    // @Path("/payments")
    // @Consumes(MediaType.APPLICATION_JSON)
    // @Produces(MediaType.APPLICATION_JSON)
    // public Uni<Response> send(@Valid PaymentRequest request) {

    //      return Uni.createFrom()
    //             .voidItem()
    //             .invoke(() -> paymentProducerRedis.publish(request))
    //             .replaceWith(Response.ok().build())
    //             ;   
    // }


    @POST
    @Path("/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response send2(PaymentRequest request) {

        paymentProducerRedis.publishAsync(request);
        return Response.ok("ok").build();
    }



    @POST
    @Path("/payments-random")
    // @RunOnVirtualThread
    public Response sendRandom(PaymentRequest request) {

        var requestRandom = new PaymentRequest(
            UUID.randomUUID(),
            request.amount()
        );

        try {
            paymentProducerRedis.publish(requestRandom);
            return Response.ok().build();
        } catch (PaymentServiceException e) {
            return Response.status(e.getStatusCode())
                    .entity(new PaymentResponse("FAILED", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new PaymentResponse("ERROR", "INTERNAL_ERROR"))
                    .build();
        }
        
    }

    

    @GET
    @Path("/payments-summary")
    public Uni<PaymentsSummary> getPaymentsSummary(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr) {

        LOG.info("### getPaymentsSummary: {}, {}", fromStr, toStr);

        Instant from = DateTimeUtils.parseToInstant(fromStr);
        Instant to = DateTimeUtils.parseToInstant(toStr);

        return redisService.getPaymentsSummary(from, to)
                .onItem().invoke(summary -> LOG.info("### Resumo de pagamentos: {}", summary))
                .onFailure().invoke(e -> LOG.error("### Erro ao obter resumo de pagamentos: {}", e.getMessage()));
    }

    
    // Health check endpoint
    @GET
    @Path("/health")
    public Response health() {
        boolean isHealthy = paymentService.isHealthy();
        return isHealthy ? 
                Response.ok("{\"status\":\"UP\"}").build() :
                Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"status\":\"DOWN\"}")
                        .build();
    }


}
