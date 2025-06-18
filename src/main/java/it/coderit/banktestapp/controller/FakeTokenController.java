package it.coderit.banktestapp.controller;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/oauth/token")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@ApplicationScoped
public class FakeTokenController {
    @POST
    public Response generateToken(@FormParam("grant_type") String grantType,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret) {
        if (!"client_credentials".equals(grantType)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "unsupported_grant_type"))
                    .build();
        }

        // Simula validazione client_id e client_secret
        if (!"my-mock-client-id".equals(clientId) || !"my-mock-client-secret".equals(clientSecret)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "invalid_client"))
                    .build();
        }

        // Risposta simulata, come farebbe il vero server CBI
        Map<String, Object> fakeResponse = Map.of(
                "access_token", "AdS1bZ1JVAsY820eB4ty9uWFbkGF0Zsjx7634R81mlZY8O3hfkQYZsayqLmV8uvR",
                "token_type", "Bearer",
                "expires_in", 3600);

        return Response.ok(fakeResponse).build();
    }
}
