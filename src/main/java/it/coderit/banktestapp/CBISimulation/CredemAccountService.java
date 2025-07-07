package it.coderit.banktestapp.CBISimulation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.coderit.banktestapp.dto.CredemAccountResponse;
import it.coderit.banktestapp.dto.CredemBalancesResponse;
import it.coderit.banktestapp.dto.CredemSingleAccountResponse;
import it.coderit.banktestapp.rest.CredemClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CredemAccountService {

    @Inject
    @RestClient
    CredemClient credemClient; // Il client REST per l'API Credem

    @Inject
    MockCbiAuthService mockCbiAuthService; // Servizio per ottenere token mockati

    private static final Logger log = LoggerFactory.getLogger(CredemAccountService.class);
    private static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    // --- HEADER E VALORI MOCKATI ---
    // Questi valori saranno usati per le chiamate al CredemClient
    private static final String MOCK_DIGEST = "SHA-256=MOCK_DIGEST_VALUE";
    private static final String MOCK_SIGNATURE = "MOCK_SIGNATURE_VALUE";
    private static final String MOCK_TPP_CERTIFICATE = "MOCK_TPP_CERTIFICATE_BASE64";
    private static final String MOCK_PSU_IP_ADDRESS = "127.0.0.1";
    private static final String MOCK_ASPSP_CODE = "MOCK_ASPSP_CODE";

    @ConfigProperty(name = "credem.psu-id")
    String psuId; // ID dell'utente (PSU) configurato

    @ConfigProperty(name = "credem.account-id")
    String defaultAccountIdForOperations; // Account ID predefinito per operazioni

    @ConfigProperty(name = "cbi.consent-mock-id")
    String mockConsentId; // ID del consenso mockato

    /**
     * Recupera la lista di tutti gli account disponibili tramite l'API Credem.
     * Utilizza header mockati per l'autenticazione e la firma.
     * @return CredemAccountResponse contenente la lista degli account.
     */
    public CredemAccountResponse fetchAccounts() {
        log.info("Tentativo di recuperare la lista degli account da CredemClient.");
        String token = mockCbiAuthService.getAccessToken(); // Ottiene il token TPP mockato

        String xRequestId = "req-" + UUID.randomUUID().toString();
        String dateHeader = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER);
        String psuAuthorization = mockCbiAuthService.getPsuAccessToken(psuId, mockConsentId);

        try {
            CredemAccountResponse accountResponse = credemClient.getAccounts(
                    psuId,
                    token, 
                    mockConsentId,
                    xRequestId,
                    dateHeader,
                    MOCK_DIGEST, 
                    MOCK_SIGNATURE, 
                    MOCK_TPP_CERTIFICATE, 
                    psuAuthorization,
                    MOCK_PSU_IP_ADDRESS,
                    MOCK_ASPSP_CODE
            );
            return accountResponse;
        } catch (Exception e) {
            log.error("Errore durante il recupero degli account dal CredemClient: {}", e.getMessage(), e);
            throw new RuntimeException("Impossibile recuperare i conti dalla banca esterna.", e);
        }
    }

    /**
     * Recupera un accountId per le operazioni. Tenta di usare un accountId preferito
     * configurato; altrimenti, usa il primo account disponibile dalla lista.
     * Se non trova alcun account, ritorna l'accountId di fallback configurato.
     * @return L'accountId da utilizzare per le operazioni.
     */
    public String getAccountIdForOperations() {
        log.info("Tentativo di recuperare un accountId dinamico da CredemClient.");
        try {
            CredemAccountResponse accountResponse = fetchAccounts();
            if (accountResponse != null && accountResponse.accounts != null && !accountResponse.accounts.isEmpty()) {
                Optional<String> preferredAccountId = accountResponse.accounts.stream()
                        .filter(account -> defaultAccountIdForOperations.equals(account.resourceId))
                        .map(account -> account.resourceId)
                        .findFirst();

                if (preferredAccountId.isPresent()) {
                    log.info("AccountId preferito '{}' recuperato con successo dalla lista degli account.",
                            preferredAccountId.get());
                    return preferredAccountId.get();
                } else {
                    String firstAccountId = accountResponse.accounts.get(0).resourceId;
                    log.warn(
                            "AccountId preferito '{}' non trovato nella lista degli account. Utilizzo il primo account disponibile: {}",
                            defaultAccountIdForOperations, firstAccountId);
                    return firstAccountId;
                }
            } else {
                log.warn(
                        "Nessun account trovato tramite CredemClient. Utilizzo l'accountId predefinito di fallback: {}",
                        defaultAccountIdForOperations);
                return defaultAccountIdForOperations;
            }
        } catch (Exception e) {
            log.error(
                    "Errore durante il recupero dinamico degli account da CredemClient. Utilizzo l'accountId predefinito di fallback: {}. Errore: {}",
                    defaultAccountIdForOperations, e.getMessage(), e);
            return defaultAccountIdForOperations;
        }
    }

    /**
     * Recupera i dettagli di un account specifico tramite l'API Credem.
     * @param accountId L'ID dell'account di cui recuperare i dettagli.
     * @param withBalance Flag per indicare se includere i saldi nei dettagli.
     * @return CredemSingleAccountResponse contenente i dettagli dell'account.
     */
    public CredemSingleAccountResponse getSpecificAccountDetails(String accountId, Boolean withBalance) {
        log.info("Tentativo di recuperare i dettagli per l'account: {} con saldo: {}", accountId, withBalance);
        try {
            String token = mockCbiAuthService.getAccessToken();
            String xRequestId = "req-" + UUID.randomUUID().toString();
            String dateHeader = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER);
            String psuAuthorization = mockCbiAuthService.getPsuAccessToken(psuId, mockConsentId);

            CredemSingleAccountResponse response = credemClient.getAccountDetails(
                    accountId,
                    mockConsentId,
                    psuId,
                    token,
                    xRequestId,
                    dateHeader,
                    MOCK_DIGEST,
                    MOCK_SIGNATURE,
                    MOCK_TPP_CERTIFICATE, 
                    psuAuthorization,
                    MOCK_PSU_IP_ADDRESS,
                    MOCK_ASPSP_CODE,
                    withBalance
            );

            if (response != null && response.account != null) {
                log.info("Dettagli account recuperati per: {}. IBAN: {}", accountId, response.account.iban);

                if (response.account.balances != null && !response.account.balances.isEmpty()) {
                    response.account.balances.forEach(balance -> {
                        log.info("Saldo disponibile: {} {} (Tipo: {})",
                                balance.amount,
                                balance.currency,
                                balance.balanceType);
                    });
                } else {
                    log.info("Nessun saldo disponibile per l'account: {}", accountId);
                }
            } else {
                log.warn("Nessun dettaglio account trovato per: {} o risposta malformata", accountId);
            }

            return response;
        } catch (Exception e) {
            log.error("Errore durante il recupero dei dettagli dell'account {}: {}", accountId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Recupera i saldi per un account specifico tramite l'API Credem.
     * @param accountId L'ID dell'account di cui recuperare i saldi.
     * @return CredemBalancesResponse contenente i saldi dell'account.
     */
    public CredemBalancesResponse getBalancesForSpecificAccount(String accountId) {
        log.info("Tentativo di recuperare i saldi per l'account: {}", accountId);
        try {
            String token = mockCbiAuthService.getAccessToken();
            String xRequestId = "req-" + UUID.randomUUID().toString();
            String dateHeader = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER);
            String psuAuthorization = mockCbiAuthService.getPsuAccessToken(psuId, mockConsentId);

            CredemBalancesResponse response = credemClient.getAccountBalances(
                    accountId,
                    mockConsentId,
                    psuId,
                    token,
                    xRequestId,
                    dateHeader,
                    MOCK_DIGEST, 
                    MOCK_SIGNATURE, 
                    MOCK_TPP_CERTIFICATE,
                    psuAuthorization,
                    MOCK_PSU_IP_ADDRESS,
                    MOCK_ASPSP_CODE
            );

            if (response != null && response.balances != null && !response.balances.isEmpty()) {
                log.info("Saldi recuperati per l'account: {}", accountId);
                response.balances.forEach(balance -> {
                    log.info("Saldo: {} {}, Tipo: {}, Data Ultimo Aggiornamento: {}",
                            balance.amount, balance.currency, balance.balanceType, balance.lastChangeDateTime);
                });
            } else {
                log.warn("Nessun saldo trovato per l'account: {} o risposta malformata", accountId);
            }
            return response;
        } catch (Exception e) {
            log.error("Errore durante il recupero dei saldi dell'account {}: {}", accountId, e.getMessage(), e);
            return null;
        }
    }
}
