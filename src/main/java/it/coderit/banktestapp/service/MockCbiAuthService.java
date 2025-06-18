package it.coderit.banktestapp.service;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import it.coderit.banktestapp.rest.CbiAuthClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MockCbiAuthService {
     
    @ConfigProperty(name = "cbi.client-id")
    String clientId;

    @ConfigProperty(name = "cbi.client-secret")
    String clientSecret;

    @Inject
    @RestClient
    CbiAuthClient authClient;

    public String getAccessToken() {
        Map<String, Object> response = authClient.getToken("client_credentials", clientId, clientSecret);
        return "Bearer " + response.get("access_token").toString();
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

}
