package com.rinha.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.model.PaymentRequest;
import com.rinha.service.PublisherService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/payments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResource.class);

    @Inject
    PublisherService publisher;

    @POST
    public Uni<Response> send(@Valid PaymentRequest request) {
        return publisher
                .publish(request)
                .onItem().transform(unused -> Response.accepted().build())
                .onFailure().invoke(t -> LOG.error("### Erro ao publicar no Redis", t))
                .onFailure()
                    .recoverWithItem(t -> Response.status(500).entity("Erro genérico: " + t.getMessage()).build());
}

}
