package it.coderit.banktestapp.rest.fake;


import java.io.InputStream;


import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.properties.IfBuildProperty;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
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
            String accountId,
            String dateFrom,
            String dateTo,
            Integer limit,
            Integer offset,
            String psuId,
            String token) {

        String resourcePath = (offset == null || offset == 0)
                ? "test-data/transaction_page_1.json"
                : "test-data/transaction_page_3.json";

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.err.println("File non trovato: " + resourcePath);
                return new CredemTransactionResponse(); // vuota
            }

            CredemTransactionResponse response = objectMapper.readValue(inputStream, CredemTransactionResponse.class);
            System.out.println("Transazioni caricate: " + response.booked.size());
            return response;

        } catch (Exception e) {
            System.err.println("Errore lettura/parsing JSON: " + e.getMessage());
            return new CredemTransactionResponse();
        }
    }
}