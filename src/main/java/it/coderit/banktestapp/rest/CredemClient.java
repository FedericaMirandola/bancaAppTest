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
        @HeadParam("PSU-ID") String psuId,
        @HeaderParam("Authorization") String token,
        @HeaderParam("X-Request-ID") String xRequestId,
        @HeaderParam("Consent-ID") String consentId,
        @HeaderParam("Date") String dateHeader,
        @HeaderParam("Digest") String digest,
        @HeaderParam("Signature") String signature,
        @HeaderParam("TPP-Signature-Certificate") String tppSignatureCertificate,
        @HeaderParam("PSU-Authorization") String psuAuthorization,
        @HeaderParam("PSU-IP-Address") String psuIpAddress,
        @HeaderParam("ASPSP-Code") String aspspCode,
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
        @HeaderParam("Authorization") String token,
        @HeaderParam("Consent-id") String consentId,
        @HeaderParam("X-Request-ID") String xRequestId,
        @HeaderParam("Date") String dateHeader
    );

    @GET
    @Path("/{accountId}")
    CredemAccountResponse getAccountDetails(
        @PathParam("accountId") String accountId,
        @HeaderParam("Consent-ID") String consentId,
        @HeaderParam("PSU-ID") String psuId,
        @HeaderParam("Authorization") String token,
        @HeaderParam("X-Request-ID") String xRequestId,
        @HeaderParam("Date") String dateHeader,
        @QueryParam("withBalance") Boolean withBalance
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
