package it.coderit.banktestapp.rest;

import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Path("/oauth/token")
@RegisterRestClient(configKey = "cbi-auth")
public interface CbiAuthClient {
    //interfaccia per simulare il client CBI per ottenere il token di accesso
    //questo client è mockato e non fa chiamate reali, ma restituisce un token statico
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> getToken(
        @FormParam("grant_type") String grantType,
        @FormParam("client_id") String clientId,
        @FormParam("client_secret") String clientSecret
    );
    
} 
