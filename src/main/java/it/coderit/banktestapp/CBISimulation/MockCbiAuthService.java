package it.coderit.banktestapp.CBISimulation;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Servizio mock per l'autenticazione CBI.
 * Fornisce token e autorizzazioni PSU hardcoded per scopi di test/sviluppo.
 */
@ApplicationScoped
public class MockCbiAuthService {
    // Token TPP fisso 
    private static final String MOCK_TPP_ACCESS_TOKEN = "Bearer MOCK_TPP_ACCESS_TOKEN_XYZ123";

    // Autorizzazione PSU fissa (simula il "PSU-Authorization" ottenuto dal redirect flow)
    
    private static final String MOCK_PSU_AUTHORIZATION = "Bearer MOCK_PSU_AUTHORIZATION_ABC456";

    // Restituisce un token di accesso TPP mockato.
    public String getAccessToken() {
        return MOCK_TPP_ACCESS_TOKEN;
    }

    
    // Restituisce un'autorizzazione PSU mockata.
    public String getPsuAccessToken(String psuId, String consentId) {
        // Possiamo includere psuId e consentId nel mock per debugging, ma la stringa è fissa
        return MOCK_PSU_AUTHORIZATION + "_PSU_" + psuId + "_CONSENT_" + consentId;
    }

    
}
