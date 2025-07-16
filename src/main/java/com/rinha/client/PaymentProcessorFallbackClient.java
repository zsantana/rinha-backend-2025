package com.rinha.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.rinha.dto.PaymentProcessorRequest;
import com.rinha.dto.PaymentProcessorResponse;

import java.util.concurrent.CompletionStage;

@RegisterRestClient(configKey = "payment-processor-fallback")
public interface PaymentProcessorFallbackClient {
    
    @POST
    @Path("/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<PaymentProcessorResponse> processPayment(PaymentProcessorRequest request);
    
}