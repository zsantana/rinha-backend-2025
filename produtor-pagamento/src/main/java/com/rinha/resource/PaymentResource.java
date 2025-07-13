package com.rinha.resource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rinha.dto.PaymentRequest;
import com.rinha.service.PaymentService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResource.class);


    @Inject
    PaymentService paymentService;

    
    @POST
    @Path("/payments")
    public Uni<Response> send(@Valid PaymentRequest request) {

        // LOG.info("### Enviando para o Redis: {}", request);
        return paymentService
                .savePayment(request)
                .onItem().ignore().andSwitchTo(Uni.createFrom().item(Response.ok().build()))
                .onFailure().recoverWithItem(th -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(th.getMessage()).build());
    }

    @GET
    @Path("/payments-summary")
    public Uni<Response> getPaymentsSummary(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr) {

        // fromStr = fromStr == null ? "2023-01-01T00:00:00Z" : fromStr;
        // toStr = toStr == null ? "2024-12-31T23:59:59Z" : toStr;

        LOG.info("### getPaymentsSummary: {}, {}", fromStr, toStr);

        Instant from;
        Instant to;

        try {
            from = parseDate(fromStr);
            to = parseDate(toStr);
        } catch (DateTimeParseException e) {
            LOG.error("Error parsing date-time: {}", e.getMessage());
            return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid date-time format. Please use ISO-8601 format.").build());
        }

        return paymentService.getPaymentsSummary(from, to)
                .onItem().transform(summary -> Response.ok(summary).build())
                .onFailure().recoverWithItem(th -> {
                    LOG.error("Error retrieving payment summary: {}", th.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Error retrieving payment summary.").build();
                });
    }



    private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .optionalStart().appendOffsetId().optionalEnd()
        .optionalStart().appendLiteral('Z').optionalEnd()
        .toFormatter();

    private Instant parseDate(String dateString) {
        try {
            TemporalAccessor ta = FLEXIBLE_FORMATTER.parseBest(dateString, ZonedDateTime::from, LocalDateTime::from);
            if (ta instanceof ZonedDateTime zdt) {
                return zdt.toInstant();
            } else if (ta instanceof LocalDateTime ldt) {
                return ldt.toInstant(ZoneOffset.UTC);
            } else {
                throw new DateTimeParseException("Unknown date-time format", dateString, 0);
            }
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Failed to parse date-time: " + dateString, dateString, 0);
        }
    }


}
