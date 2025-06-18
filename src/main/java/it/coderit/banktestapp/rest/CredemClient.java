package it.coderit.banktestapp.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import it.coderit.banktestapp.dto.CredemTransactionResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;


@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "credem-api")
public interface CredemClient {
    
    @GET
    @Path("/{accountId}/transactions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    CredemTransactionResponse getTransactions(
        @PathParam("accountId") String accountId,
        @QueryParam("dateFrom") String dateFrom,
        @QueryParam("dateTo") String dateTo,
        @QueryParam("limit") Integer limit,     
        @QueryParam("offset") Integer offset,
        @HeaderParam("psu-id") String psuId,
        @HeaderParam("Authorization") String token 
    );
}
