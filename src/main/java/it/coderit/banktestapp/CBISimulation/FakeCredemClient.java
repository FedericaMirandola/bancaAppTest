package it.coderit.banktestapp.CBISimulation;

import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.properties.IfBuildProperty;
import it.coderit.banktestapp.dto.CredemAccountResponse;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.dto.CredemSingleAccountResponse;
import it.coderit.banktestapp.dto.CredemBalancesResponse;
import it.coderit.banktestapp.rest.CredemClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@IfBuildProperty(name = "use.fake.credem", stringValue = "true") // usa solo se configurato
public class FakeCredemClient implements CredemClient {

    @Inject
    ObjectMapper objectMapper;

    @Override
    public CredemTransactionResponse getTransactions(
            String psuId,
            String authorization, 
            String xRequestId,
            String consentId,
            String date, 
            String digest, 
            String signature, 
            String tppSignatureCertificate,
            String psuAuthorization,
            String psuIpAddress, 
            String aspspCode, 
            String accountId,
            String fromBookingDate, 
            String toBookingDate, 
            Integer limit,
            Integer offset) {


        String resourcePath;
        if (offset == null || offset == 0) {
            resourcePath = "test-data/transaction_page_1.json";
        } else if (offset == 100) {
            resourcePath = "test-data/transaction_page_2.json";
        } else if (offset == 200) {
            resourcePath = "test-data/transaction_page_3.json";
        } else if (offset == 300) {
            resourcePath = "test-data/transaction_page_4.json";
        } else {
           return new CredemTransactionResponse(); //restituisce lista vuota
        }

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.err.println("File non trovato: " + resourcePath);
                return new CredemTransactionResponse(); // vuota
            }

            CredemTransactionResponse response = objectMapper.readValue(inputStream, CredemTransactionResponse.class);
            System.out.println("Transazioni caricate da " + resourcePath + ": " + (response.booked != null ? response.booked.size() : 0));
            return response;

        } catch (Exception e) {
            System.err.println("Errore lettura/parsing JSON: " + e.getMessage());
            return new CredemTransactionResponse();
        }
    }

    @Override
    public CredemAccountResponse getAccounts(
            String psuId,
            String authorization, 
            String consentId,
            String xRequestId,
            String date, 
            String digest, 
            String signature, 
            String tppSignatureCertificate, 
            String psuAuthorization, 
            String psuIpAddress, 
            String aspspCode 
            ) {


        String resourcePath = "test-data/accounts.json";

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.err.println("File account non trovato: " + resourcePath);
                return new CredemAccountResponse();
            }

            CredemAccountResponse response = objectMapper.readValue(inputStream, CredemAccountResponse.class);
            System.out.println("Account caricati da " + resourcePath + ": " + (response.accounts != null ? response.accounts.size() : 0));
            return response;

        } catch (Exception e) {
            System.err.println("Errore lettura/parsing JSON account da " + resourcePath + ": " + e.getMessage());
            return new CredemAccountResponse();
        }
    }

    @Override
    public CredemSingleAccountResponse getAccountDetails(
            String accountId,
            String consentId,
            String psuId,
            String authorization, 
            String xRequestId,
            String date, 
            String digest, 
            String signature, 
            String tppSignatureCertificate, 
            String psuAuthorization, 
            String psuIpAddress, 
            String aspspCode, 
            Boolean withBalance) {

            String resourcePath;

        if (Boolean.TRUE.equals(withBalance)) {
            resourcePath = "test-data/account_details_with_balance_" + accountId + ".json"; // Aggiunto underscore per coerenza con il nome file
        } else {
            resourcePath = "test-data/account_details_with_no_balance_" + accountId + ".json"; // Aggiunto underscore
        }

        InputStream tmpInputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (tmpInputStream == null) {
            System.err.println("File mock account details non trovato: " + resourcePath + ". Tentativo con file generico");
            resourcePath = "test-data/account_details_generic.json"; // fallback
            tmpInputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        }

        final InputStream inputStream = tmpInputStream;
        try (inputStream) {
            if (inputStream == null) {
                System.err.println("File mock account details generico non trovato: " + resourcePath);
                return new CredemSingleAccountResponse(); // vuota
            }

            CredemSingleAccountResponse response = objectMapper.readValue(inputStream, CredemSingleAccountResponse.class);
            System.out.println("Dettagli account caricati da " + resourcePath + ": " + response.account.resourceId);
            return response;

        } catch (Exception e) {
            System.err.println("Errore lettura/parsing JSON account details: " + e.getMessage());
            return new CredemSingleAccountResponse();
        }
    }

    @Override
    public CredemBalancesResponse getAccountBalances(
            String accountId,
            String consentId,
            String psuId,
            String authorization, 
            String xRequestId,
            String date, 
            String digest, 
            String signature, 
            String tppSignatureCertificate,
            String psuAuthorization, 
            String psuIpAddress, 
            String aspspCode 
            ) {


        String resourcePath = "test-data/account_balances_" + accountId + ".json";

        InputStream tmpInputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (tmpInputStream == null) {
            System.err.println("File mock balances specifico non trovato: " + resourcePath);
            resourcePath = "test-data/account_balances_generic.json"; // fallback
            tmpInputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        }

        final InputStream inputStream = tmpInputStream;
        try (inputStream) {
            if (inputStream == null) {
                System.err.println("File mock balances generico non trovato: " + resourcePath);
                return new CredemBalancesResponse(); // vuota
            }

            CredemBalancesResponse response = objectMapper.readValue(inputStream, CredemBalancesResponse.class);
            System.out.println("Saldi account caricati da " + resourcePath + ": " + (response.balances != null ? response.balances.size() : 0));
            return response;

        } catch (Exception e) {
            System.err.println("Errore lettura/parsing JSON balances: " + e.getMessage());
            return new CredemBalancesResponse();
        }
    }
}
