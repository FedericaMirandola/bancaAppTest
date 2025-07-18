package it.coderit.banktestapp.rest;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import it.coderit.banktestapp.dto.CredemAccountResponse;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.dto.CredemBalancesResponse;
import it.coderit.banktestapp.dto.CredemSingleAccountResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;


@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "credem-api")
public interface CredemClient {
    
    @GET
        @Path("/{accountId}/transactions")
        CredemTransactionResponse getTransactions(
                @HeaderParam("PSU-ID") String psuId,
                @HeaderParam("Authorization") String authorization, 
                @HeaderParam("X-Request-ID") String xRequestId,
                @HeaderParam("Consent-ID") String consentId,
                @HeaderParam("Date") String date,
                @HeaderParam("Digest") String digest, 
                @HeaderParam("Signature") String signature, 
                @HeaderParam("TPP-Signature-Certificate") String tppSignatureCertificate, 
                @HeaderParam("PSU-Authorization") String psuAuthorization,
                @HeaderParam("PSU-IP-Address") String psuIpAddress,
                @HeaderParam("ASPSP-Code") String aspspCode,
                @PathParam("accountId") String accountId,
                @QueryParam("fromBookingDate") String fromBookingDate,
                @QueryParam("toBookingDate") String toBookingDate,
                @QueryParam("limit") Integer limit,
                @QueryParam("offset") Integer offset);

        @GET
        CredemAccountResponse getAccounts(
                @HeaderParam("PSU-ID") String psuId,
                @HeaderParam("Authorization") String authorization, // Bearer token
                @HeaderParam("Consent-ID") String consentId,
                @HeaderParam("X-Request-ID") String xRequestId,
                @HeaderParam("Date") String date,
                @HeaderParam("Digest") String digest,
                @HeaderParam("Signature") String signature, 
                @HeaderParam("TPP-Signature-Certificate") String tppSignatureCertificate, 
                @HeaderParam("PSU-Authorization") String psuAuthorization, 
                @HeaderParam("PSU-IP-Address") String psuIpAddress, 
                @HeaderParam("ASPSP-Code") String aspspCode 
        );

        @GET
        @Path("/{accountId}")
        CredemSingleAccountResponse getAccountDetails(
                @PathParam("accountId") String accountId,
                @HeaderParam("Consent-ID") String consentId,
                @HeaderParam("PSU-ID") String psuId,
                @HeaderParam("Authorization") String authorization,
                @HeaderParam("X-Request-ID") String xRequestId,
                @HeaderParam("Date") String date,
                @HeaderParam("Digest") String digest,
                @HeaderParam("Signature") String signature,
                @HeaderParam("TPP-Signature-Certificate") String tppSignatureCertificate,
                @HeaderParam("PSU-Authorization") String psuAuthorization,
                @HeaderParam("PSU-IP-Address") String psuIpAddress,
                @HeaderParam("ASPSP-Code") String aspspCode,
                @QueryParam("withBalance") Boolean withBalance);

        @GET
        @Path("/{accountId}/balances")
        CredemBalancesResponse getAccountBalances(
                @PathParam("accountId") String accountId,
                @HeaderParam("Consent-ID") String consentId,
                @HeaderParam("PSU-ID") String psuId,
                @HeaderParam("Authorization") String authorization,
                @HeaderParam("X-Request-ID") String xRequestId,
                @HeaderParam("Date") String date,
                @HeaderParam("Digest") String digest,
                @HeaderParam("Signature") String signature,
                @HeaderParam("TPP-Signature-Certificate") String tppSignatureCertificate, 
                @HeaderParam("PSU-Authorization") String psuAuthorization, 
                @HeaderParam("PSU-IP-Address") String psuIpAddress, 
                @HeaderParam("ASPSP-Code") String aspspCode 
        );
}
