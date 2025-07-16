package com.rinha.exception;

import com.rinha.dto.PaymentResponse;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

@Provider
public class TimeoutExceptionMapper implements ExceptionMapper<ProcessingException> {

    private static final Logger log = LoggerFactory.getLogger(TimeoutExceptionMapper.class);

    @Override
    public Response toResponse(ProcessingException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof TimeoutException || exception.getMessage().toLowerCase().contains("timeout")) {
            log.error("Timeout detected: {}", exception.getMessage());

            PaymentResponse errorResponse = new PaymentResponse("FAILED", "Request timed out");

            return Response.status(Response.Status.GATEWAY_TIMEOUT) // 504
                    .entity(errorResponse)
                    .build();
        }

        // Não é timeout? Deixa outra camada tratar
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new PaymentResponse("FAILED", "Internal error"))
                .build();
    }
}
