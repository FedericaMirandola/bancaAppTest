package it.coderit.banktestapp.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import it.coderit.banktestapp.dto.CredemAccountResponse;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.dto.CredemBalancesResponse;
import it.coderit.banktestapp.dto.CredemSingleAccountResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;
import jakarta.ws.res.PathParam;
import jakarta.ws.rs.QueryParam;


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

    @GET
    CredemAccountResponse getAccounts (
        @HeaderParam("psu-id") String psuId,
        @HeaderParam("Authorization") String token
    );

    @GET
    @Path("/{accountId}")
    CredemAccountResponse getSingleAccount(
        @PathParam("account-id") String accountId,
        @HeaderParam("Consent-id") String consentId,
        @QuesryParam("with-balance") Boolean withBalance,
        @HeaderParam("psu-id") String psuId,
        @HeaderParam("Authorization") String token
    );

    @GET
    @Path("/{accountId}/balances")
    CredemBalancesResponse getBalances(
        @PathParam("account-id") String accountId,
        @HeaderParam("Consent-ID") String consentId,
        @HeaderParam("PSU-ID") String psuId,
        @HeaderParam("Authorization") String token,
        @HeaderParam("X-Request-ID") String xRequestId,
        @HeaderParam("Date") String dateHeader
    );
}
