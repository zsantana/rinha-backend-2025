package com.rinha.resource;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import com.rinha.util.DateTimeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;
import com.rinha.dto.PaymentResponse;
import com.rinha.exception.PaymentServiceException;
import com.rinha.service.HighPerformancePaymentService;
import com.rinha.service.PaymentProducerService;
import com.rinha.service.PaymentService;
import com.rinha.service.PaymentoProdutorEventBus;
import com.rinha.service.RedisService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResource.class);


    // @Inject
    // PaymentService paymentService;

    @Inject
    HighPerformancePaymentService highPerformancePaymentService;

    @Inject
    PaymentProducerService paymentProducerService;

    @Inject
    PaymentoProdutorEventBus paymentoProdutorEventBus;

    @Inject
    RedisService redisService;

    
    
    @POST
    @Path("/payments")
    @RunOnVirtualThread
    public Response sendRedis2(@Valid PaymentRequest request) {

        try {
            paymentoProdutorEventBus.publishLocal(request);
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

    @POST
    @Path("/payments-random")
    // @RunOnVirtualThread
    public Response sendRandom(@Valid PaymentRequest request) {

        var requestRandom = new PaymentRequest(
            UUID.randomUUID(),
            request.amount()
        );

        try {
            paymentoProdutorEventBus.publishLocal(requestRandom);
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



    
    // @POST
    // @Path("/payments1")
    // public Uni<Response> send(@Valid PaymentRequest request) {

    //     // LOG.info("### Enviando para o Redis: {}", request);
    //     return paymentService
    //             .savePayment(request)
    //             .onItem().ignore().andSwitchTo(Uni.createFrom().item(Response.ok().build()))
    //             .onFailure().recoverWithItem(th -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(th.getMessage()).build());
    // }

    

    @GET
    @Path("/payments-summary")
    public Uni<Response> getPaymentsSummary(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr) {

        // fromStr = fromStr == null ? "2023-01-01T00:00:00Z" : fromStr;
        // toStr = toStr == null ? "2024-12-31T23:59:59Z" : toStr;

        LOG.info("### getPaymentsSummary: {}, {}", fromStr, toStr);

        Instant from = DateTimeUtils.parseToInstant(fromStr);
        Instant to = DateTimeUtils.parseToInstant(fromStr);

        return redisService.getPaymentsSummary(from, to)
                .onItem().transform(summary -> Response.ok(summary).build())
                .onFailure().recoverWithItem(th -> {
                    LOG.error("Error retrieving payment summary: {}", th.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error retrieving payment summary.").build();
                });
    }

    // @POST
    // @Path("/payments2")
    // @RunOnVirtualThread
    // public Response processPaymentSync(PaymentRequest request) {
        
    //     try {
    //         PaymentResponse response = highPerformancePaymentService.processPayment(request);
    //         return Response.ok(response).build();
    //     } catch (PaymentServiceException e) {
    //         return Response.status(e.getStatusCode())
    //                 .entity(new PaymentResponse("FAILED", e.getMessage()))
    //                 .build();
    //     } catch (Exception e) {
    //         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //                 .entity(new PaymentResponse("ERROR", "INTERNAL_ERROR"))
    //                 .build();
    //     }
    // }

    // Endpoint assíncrono para máxima performance
    // @POST
    // @Path("/payments3")
    // public CompletionStage<Response> processPaymentAsync(PaymentRequest request) {
    //     return highPerformancePaymentService.processPaymentAsync(request)
    //             .thenApply(response -> Response.ok(response).build())
    //             .exceptionally(throwable -> {
    //                 if (throwable.getCause() instanceof PaymentServiceException pse) {
    //                     LOG.info("PaymentServiceException: {}, ", pse.getMessage(), pse.getStatusCode());
    //                     return Response.status(pse.getStatusCode())
    //                             .entity(new PaymentResponse("FAILED", pse.getMessage()))
    //                             .build();
    //                 }
    //                 return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //                         .entity(new PaymentResponse("ERROR", "ASYNC_ERROR"))
    //                         .build();
    //             });
    // }



    // Endpoint para batch processing
    // @POST
    // @Path("/batch")
    // @RunOnVirtualThread
    // public CompletionStage<Response> processPaymentBatch(PaymentRequest[] requests) {
    //     if (requests == null || requests.length == 0) {
    //         return CompletableFuture.completedFuture(
    //             Response.status(Response.Status.BAD_REQUEST)
    //                     .entity("Empty batch request")
    //                     .build());
    //     }

    //     return highPerformancePaymentService.processPaymentBatch(requests)
    //             .thenApply(responses -> Response.ok(responses).build())
    //             .exceptionally(throwable -> 
    //                 Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //                         .entity("Batch processing failed")
    //                         .build());
    // }

    // Health check endpoint
    @GET
    @Path("/health")
    public Response health() {
        boolean isHealthy = highPerformancePaymentService.isHealthy();
        return isHealthy ? 
                Response.ok("{\"status\":\"UP\"}").build() :
                Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"status\":\"DOWN\"}")
                        .build();
    }


}
