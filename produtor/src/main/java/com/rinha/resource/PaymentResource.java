package com.rinha.resource;


import com.rinha.dto.PaymentRequest;
import com.rinha.service.PaymentProducerRedis;
import com.rinha.service.RedisService;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

@Path("")
public class PaymentResource {

    @Inject
    PaymentProducerRedis paymentProducerRedis;

    @Inject
    RedisService redisService;
    
    @POST
    @Path("/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response send2(PaymentRequest request) {

        paymentProducerRedis.publishAsync(request);
        return Response.ok("ok").build();

    }

    /**
     * Endpoint reativo usando multi-thread para publicação no Redis
     * Retorna Uni<Response> para processamento assíncrono completo
     */
    // @POST
    // @Path("/payments")
    // @Consumes(MediaType.APPLICATION_JSON)
    // @Produces(MediaType.APPLICATION_JSON)
    // public Uni<Response> sendMultiThread(PaymentRequest request) {
        
    //     return paymentProducerRedis.publishMultiThread(request)
    //         .map(v -> Response.ok().entity("{\"status\":\"success\",\"message\":\"Payment published successfully\"}").build())
    //         .onFailure().recoverWithItem(failure -> 
    //             Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //                 .entity("{\"status\":\"error\",\"message\":\"" + failure.getMessage() + "\"}")
    //                 .build()
    //         );
    // }

    /**
     * Endpoint síncrono usando multi-thread mas aguardando conclusão
     * Útil quando se precisa garantir que a publicação foi concluída antes de retornar
     */
    // @POST
    // @Path("/payments")
    // @Consumes(MediaType.APPLICATION_JSON)
    // @Produces(MediaType.APPLICATION_JSON)
    // @RunOnVirtualThread
    // public Response sendSyncMultiThread(PaymentRequest request) {
        
    //     try {
    //         paymentProducerRedis.publishMultiThread(request)
    //             .await().indefinitely(); // Aguarda conclusão de forma síncrona
                
    //         return Response.ok()
    //             .entity("{\"status\":\"success\",\"message\":\"Payment published and confirmed\"}")
    //             .build();
                
    //     } catch (Exception e) {
    //         return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
    //             .entity("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}")
    //             .build();
    //     }
    // }


}