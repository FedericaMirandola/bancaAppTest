package it.coderit.banktestapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.repository.TransactionRepository;
import it.coderit.banktestapp.model.CenterType;
import it.coderit.banktestapp.service.TransactionService;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@QuarkusTest
public class TransactionControllerTest {

    @InjectMock
    TransactionService transactionService;

    @InjectMock
    TransactionRepository transactionRepository;

    private final String DEFAULT_TEST_ACCOUNT_ID = "IT001000000000000000001";
    private static final String VALID_USERNAME = "user";
    private static final String VALID_PASSWORD = "userpassword";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "adminpassword";
    private static final String INVALID_USERNAME = "invalid";
    private static final String INVALID_PASSWORD = "password";


    private Transaction transactionTestCosto;
    private Transaction transactionTestProfitto;
    private Transaction transactionTestCosto2;
    private Transaction transactionTestUndefined;

    @BeforeEach
    public void setUp() {
        Mockito.reset(transactionService, transactionRepository);

        transactionTestCosto = new Transaction();
        transactionTestCosto.setTransactionId(UUID.randomUUID().toString());
        transactionTestCosto.setAccountId(DEFAULT_TEST_ACCOUNT_ID);
        transactionTestCosto.setBookingDate(OffsetDateTime.parse("2023-10-01T10:00:00Z"));
        transactionTestCosto.setAmount(BigDecimal.valueOf(-50.0));
        transactionTestCosto.setRemittanceInformation("Test Transaction Costo");
        transactionTestCosto.setCenterType(CenterType.COSTO);
        transactionTestCosto.setIsManuallyClassified(false);
        transactionTestCosto.setCurrency("EUR");

        transactionTestProfitto = new Transaction();
        transactionTestProfitto.setTransactionId(UUID.randomUUID().toString());
        transactionTestProfitto.setAccountId(DEFAULT_TEST_ACCOUNT_ID);
        transactionTestProfitto.setBookingDate(OffsetDateTime.parse("2023-10-15T12:00:00Z"));
        transactionTestProfitto.setAmount(BigDecimal.valueOf(100.0));
        transactionTestProfitto.setRemittanceInformation("Test Transaction Profitto");
        transactionTestProfitto.setCenterType(CenterType.PROFITTO);
        transactionTestProfitto.setIsManuallyClassified(false);
        transactionTestProfitto.setCurrency("EUR");

        transactionTestCosto2 = new Transaction();
        transactionTestCosto2.setTransactionId(UUID.randomUUID().toString());
        transactionTestCosto2.setAccountId(DEFAULT_TEST_ACCOUNT_ID);
        transactionTestCosto2.setBookingDate(OffsetDateTime.parse("2023-10-15T12:00:00Z"));
        transactionTestCosto2.setRemittanceInformation("Altro costo");
        transactionTestCosto2.setCenterType(CenterType.COSTO);
        transactionTestCosto2.setIsManuallyClassified(false);
        transactionTestCosto2.setCurrency("EUR");

        transactionTestUndefined = new Transaction();
        transactionTestUndefined.setId(3L);
        transactionTestUndefined.setAccountId("testAccountId");
        transactionTestUndefined.setTransactionId("trans_id_undefined"); // GiÃ  presente, ottimo!
        transactionTestUndefined.setBookingDate(OffsetDateTime.parse("2023-11-01T09:00:00Z"));
        transactionTestUndefined.setAmount(BigDecimal.valueOf(25.0));
        transactionTestUndefined.setRemittanceInformation("Transazione Sconosciuta");
        transactionTestUndefined.setCenterType(CenterType.UNDEFINED);
        transactionTestUndefined.setIsManuallyClassified(false);
        transactionTestUndefined.setCurrency("EUR");

        when(transactionService.searchTransactions(anyString(), any(LocalDate.class), any(LocalDate.class),
                any(CenterType.class)))
                .thenReturn(List.of(transactionTestCosto, transactionTestProfitto, transactionTestUndefined));

        when(transactionService.searchTransactions(eq("testAccountId"), eq(null), eq(null), eq(null)))
                .thenReturn(List.of(transactionTestCosto, transactionTestProfitto, transactionTestUndefined));

        doNothing().when(transactionService).downloadAndSave(anyString(), anyString(), anyString());
        doNothing().when(transactionService).saveTransactionsFromDTOList(any(List.class), anyString());

        // 1. Mock per find("transactionId", "trans_id_undefined") che trova la transazione
        PanacheQuery<Transaction> mockQueryFound = Mockito.mock(PanacheQuery.class);
        when(mockQueryFound.firstResult()).thenReturn(transactionTestUndefined); // Restituisce l'oggetto Transaction
        // Assicurati che il mock 'find' venga chiamato con il nome del campo e il valore
        when(transactionRepository.find(eq("transactionId"), eq("trans_id_undefined"))).thenReturn(mockQueryFound);

        // 2. Mock per find("transactionId", "non_esistente") che non trova la transazione
        PanacheQuery<Transaction> mockQueryNotFound = Mockito.mock(PanacheQuery.class);
        when(mockQueryNotFound.firstResult()).thenReturn(null); // Restituisce null per indicare non trovato
        when(transactionRepository.find(eq("transactionId"), eq("non_esistente"))).thenReturn(mockQueryNotFound);

        // 3. Mock per find("transactionId", "trans_id_error") che simula un errore nel DB
        PanacheQuery<Transaction> mockQueryError = Mockito.mock(PanacheQuery.class);
        when(mockQueryError.firstResult()).thenThrow(new RuntimeException("Errore DB simulato durante la ricerca"));
        when(transactionRepository.find(eq("transactionId"), eq("trans_id_error"))).thenReturn(mockQueryError);
        
        // --- FINE CORREZIONI ---

        // Mock per la persistenza (update)
        doNothing().when(transactionRepository).persist(any(Transaction.class));

    }

    // --- TEST per l'ENDPOINT GET /transactions ---

    // deve restituire tutte le transazoni quando non sono formiti parametri
    @Test
    void transactions_shouldReturnTransactions_whenNoQueryParams() {

        when(transactionService.searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null), eq(null)))
                .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(2))
                .body("[0].remittanceInformation", is(transactionTestCosto.getRemittanceInformation()))
                .body("[1].remittanceInformation", is(transactionTestProfitto.getRemittanceInformation()));

        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null),
                eq(null));
    }

    // deve ritornare una lista di transazioni quando un range di date è formito
    @Test
    void transactions_shouldReturnFilteredTransactions_whenDateRangeProvided() {
        LocalDate fromDate = LocalDate.of(2023, 1, 1);
        LocalDate toDate = LocalDate.of(2023, 1, 31);

        when(transactionService.searchTransactions(DEFAULT_TEST_ACCOUNT_ID, fromDate, toDate, null))
                .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

        given()
                .auth().basic("user", "userpassword")
                .queryParam("from", "2023-01-01")
                .queryParam("to", "2023-01-31")
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(2));

        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(fromDate), eq(toDate),
                eq(null));
    }

    // deve restituire una lista di transazioni quando un "Tipo Centro" è fornito
    @Test
    void transactions_shouldReturnFilteredTransactions_whenCenterTypeIsProvided() {
        when(transactionService.searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null),
                eq(CenterType.COSTO)))
                .thenReturn(List.of(transactionTestCosto, transactionTestCosto2));

        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .queryParam("centerType", "COSTO")
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(2))
                .body("[0].centerType", is(CenterType.COSTO.name()))
                .body("[1].centerType", is(CenterType.COSTO.name()));

        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null),
                eq(CenterType.COSTO));

    }

    // deve ritornare una lista di transazioni quando range di date e tipo Centro
    // sono entrambi forniti
    @Test
    void transactions_shouldReturnFilteredTransactions_whenCenterTypeAndDatesAreProvided() {
        LocalDate fromDate = LocalDate.of(2023, 1, 1);
        LocalDate toDate = LocalDate.of(2023, 1, 31);

        when(transactionService.searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(fromDate), eq(toDate),
                eq(CenterType.PROFITTO)))
                .thenReturn(List.of(transactionTestProfitto));

        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .queryParam("from", "2023-01-01")
                .queryParam("to", "2023-01-31")
                .queryParam("centerType", "PROFITTO")
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(1))
                .body("[0].remittanceInformation", is(transactionTestProfitto.getRemittanceInformation()))
                .body("[0].centerType", is(CenterType.PROFITTO.name()));

        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(fromDate), eq(toDate),
                eq(CenterType.PROFITTO));
    }

    // deve restituire una lista vuota quando non trova nessuna transazione
    @Test
    void transactions_shouldReturnEmptyList_whenNoTransactionIsFound() {
        when(transactionService.searchTransactions(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .when()
                .get("/transactions")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(0))
                .body("", is(empty()));

        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null),
                eq(null));
    }

    // deve restituire errore 400 quando viene fornita solo la data From
    @Test
    void transactions_shouldReturnBadRequest_whenOnlyDateFromIsProvided() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .queryParam("from", "2023-01-01")
                .when()
                .get("/transactions")
                .then()
                .statusCode(400)
                .body(is("Per il filtro per data, entrambi i parametri 'from' e 'to' sono obbligatori."));

        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());
    }

    // deve restituire errore 400 quando viene fornita solo la data To
    @Test
    void transactions_shouldReturnBadrequest_whenOnlyDateToIsProvided() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .queryParam("to", "2023-01-31")
                .when()
                .get("/transactions")
                .then()
                .statusCode(400)
                .body(is("Per il filtro per data, entrambi i parametri 'from' e 'to' sono obbligatori."));

        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());

    }

    // deve restituire errore 400 quando il fomato data non è valido
    @Test
    void transactions_shouldReturnbadRequest_whenDateFormatInvalid() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .queryParam("from", "2023/01/01")
                .queryParam("to", "2023-01-31")
                .when()
                .get("/transactions")
                .then()
                .statusCode(400)
                .body(is("Formato data non valido per 'from' o 'to'. Utilizzare il formato YYYY-MM-DD."));

        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());

    }

    // deve restituire errore 400 quando le date from e to sono invertite
    @Test
    void transactions_shouldReturnBadrequest_whenDatesAreInverted() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .queryParam("from", "2023-01-31")
                .queryParam("to", "2023-01-01")
                .when()
                .get("/transactions")
                .then()
                .statusCode(400)
                .body(is("La data 'from' non può essere successiva alla data 'to'."));

        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());
    }

    // deve restituire errore 400 quando il tipo Centro non è valido
    @Test
    void transactions_shouldReturnBadRequest_whenCenterTypeIsInvalid() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .queryParam("centerType", "INVALID_TYPE")
                .when()
                .get("/transactions")
                .then()
                .statusCode(400)
                .body(is("Tipo center non valido. Valori possibili: COSTO, PROFITTO, UNDEFINED"));

        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());
    }

    // deve retituire errore 500 quando viene lanciata un eccezione
    @Test
    void transactions_shouldreturnInternalServerError_whenServiceThrowException() {
        when(transactionService.searchTransactions(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Errore server simulato"));

        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .when()
                .get("/transactions")
                .then()
                .statusCode(500)
                .body(is("Errore durante il recupero/filtro dei movimenti: Errore server simulato"));
    }

    // deve restituire errore 401 non autorizzato quando non si effettua
    // l'autenticazione
    @Test
    void transactions_shouldReturnUnathorized_whenNotAuthenticated() {
        given()
                .when()
                .get("/transactions")
                .then()
                .statusCode(401);

        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());
    }

    @Test
    void manualClassifyTransaction_shouldUpdateCenterTypeAndSetManualFlag_whenValid() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .contentType(ContentType.JSON)
                .queryParam("centerType", "PROFITTO")
                .when()
                .put("/movimenti/{transactionId}/manual-classify", "trans_id_undefined")
                .then()
                .statusCode(200)
                .body(is("Transazione ID 'trans_id_undefined' classificata manualmente come 'PROFITTO'."));

        // Verifica che il repository sia stato chiamato per trovare la transazione
        verify(transactionRepository, times(1)).find("transactionId", "trans_id_undefined");

        // Verifica che la transazione sia stata persistita con i nuovi valori usando
        // argThat
        verify(transactionRepository, times(1)).persist((Transaction) argThat(transaction -> {
            Transaction t = (Transaction) transaction;
            return t.getCenterType() == CenterType.PROFITTO && // Verifica il centerType
                    t.getIsManuallyClassified() == true && // Verifica il flag manuale
                    "trans_id_undefined".equals(t.getTransactionId()); // Verifica l'ID transazione
        }));
    }

    @Test
    void manualClassifyTransaction_shouldReturnNotFound_whenTransactionDoesNotExist() {
        given() 
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .contentType(ContentType.JSON)
                .queryParam("centerType", "COSTO")
                .when()
                .put("/movimenti/{transactionId}/manual-classify", "non_esistente")
                .then()
                .statusCode(404)
                .body(containsString("Transazione con ID 'non_esistente' non trovata."));

       // verify(transactionRepository, times(1)).find("transactionId", "non_esistente");
       // verify(transactionRepository, never()).persist(any(Transaction.class)); // Non deve persistere nulla
    }

    @Test
    void manualClassifyTransaction_shouldReturnBadRequest_whenCenterTypeIsMissing() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .contentType(ContentType.JSON)
                // Nessun queryParam "centerType"
                .when()
                .put("/transactions/{transactionId}/manual-classify", "trans_id_undefined")
                .then()
                .statusCode(400)
                .body(containsString("Il parametro 'centerType' è obbligatorio e non può essere vuoto."));

        verify(transactionRepository, never()).find("transactionId", "trans_id_undefined"); // Nessuna interazione col DB
        verify(transactionRepository, never()).persist(any(Transaction.class));
    }

    @Test
    void manualClassifyTransaction_shouldReturnBadRequest_whenInvalidCenterType() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .contentType(ContentType.JSON)
                .queryParam("centerType", "INVALIDO") // Tipo non valido
                .when()
                .put("/transactions/{transactionId}/manual-classify", "trans_id_undefined")
                .then()
                .statusCode(400)
                .body(containsString("Tipo center non valido. Valori possibili: COSTO, PROFITTO."));

        verify(transactionRepository, never()).find("transactionId", "trans_id_undefined");
        verify(transactionRepository, never()).persist(any(Transaction.class));
    }

    @Test
    void manualClassifyTransaction_shouldReturnBadRequest_whenCenterTypeIsUndefined() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .contentType(ContentType.JSON)
                .queryParam("centerType", "UNDEFINED") // Non permesso per classificazione manuale
                .when()
                        .put("/transactions/{transactionId}/manual-classify", "trans_id_undefined")
                .then()
                        .statusCode(400)
                        .body(containsString(
                        "Non puoi classificare manualmente una transazione come 'UNDEFINED'. Usa 'COSTO' o 'PROFITTO'."));

        verify(transactionRepository, never()).find("transactionId", "trans_id_undefined");
        verify(transactionRepository, never()).persist(any(Transaction.class));
    }

    @Test
    void manualClassifyTransaction_shouldReturnInternalServerError_whenRepositoryFails() {
        given()
                .auth().preemptive().basic(VALID_USERNAME, VALID_PASSWORD)
                .contentType(ContentType.JSON)
                .queryParam("centerType", "COSTO")
                .when()
                        .put("/transactions/{transactionId}/manual-classify", "trans_id_error")
                .then()
                        .statusCode(500)
                        .body(containsString(
                        "Errore durante la classificazione manuale della transazione: Errore DB simulato durante la ricerca"));

        verify(transactionRepository, times(1)).find("transactionId", "trans_id_error");
        verify(transactionRepository, never()).persist(any(Transaction.class));
    }

}
