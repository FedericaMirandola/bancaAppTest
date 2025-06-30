package it.coderit.banktestapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.CenterType;
import it.coderit.banktestapp.service.TransactionService;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@QuarkusTest
public class TransactionControllerTest {

    @InjectMock
    TransactionService transactionService;

    @ConfigProperty(name = "credem.account-id")
    String accountId;

    private Transaction transactionTestCosto;
    private Transaction transactionTestProfitto;

    @BeforeEach
    public void setUp() {
        Mockito.reset(transactionService);

        transactionTestCosto = new Transaction();
        transactionTestCosto.setId(1L);
        transactionTestCosto.setAccountId("testAccountId");
        transactionTestCosto.setBookingDate(OffsetDateTime.parse("2023-10-01T10:00:00Z"));
        transactionTestCosto.setAmount(BigDecimal.valueOf(100.0));
        transactionTestCosto.setRemittanceInformation("Test Transaction Costo");
        transactionTestCosto.setCenterType(CenterType.COSTO);
        transactionTestCosto.setCurrency("EUR");

        transactionTestProfitto = new Transaction();
        transactionTestProfitto.setId(2L);
        transactionTestProfitto.setAccountId("testAccountId");
        transactionTestProfitto.setBookingDate(OffsetDateTime.parse("2023-10-15T12:00:00Z"));
        transactionTestProfitto.setAmount(BigDecimal.valueOf(-50.0));
        transactionTestProfitto.setRemittanceInformation("Test Transaction Profitto");
        transactionTestProfitto.setCenterType(CenterType.PROFITTO);
        transactionTestProfitto.setCurrency("EUR");

        when(transactionService.searchTransactions(anyString(), any(LocalDate.class), any(LocalDate.class), any(CenterType.class)))
            .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

        when(transactionService.searchTransactions(eq("testAccountId"), eq(null), eq(null), eq(null)))
            .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

        when(transactionService.searchTransactions(eq("testAccountId"), any(LocalDate.class), any(LocalDate.class), eq(CenterType.COSTO)))
            .thenReturn(List.of(transactionTestCosto));

        when(transactionService.searchTransactions(eq("testAccountId"), any(LocalDate.class), any(LocalDate.class), eq(CenterType.PROFITTO)))
            .thenReturn(List.of(transactionTestProfitto));


        doNothing().when(transactionService).downloadAndSave(anyString(), anyString(), anyString());
        doNothing().when(transactionService).saveTransactionsFromDTOList(any(List.class), anyString());
    }

    // --- Tutti i test relativi agli endpoint commentati nel controller sono ora commentati qui ---

    /*
    @Test
    void getTransactions_shouldReturnTransactions_ifFound() {
        when(transactionService.searchTransactions(
            eq(accountId),
            eq(LocalDate.parse("2023-10-01")),
            eq(LocalDate.parse("2023-10-31")),
            eq(null)
        )).thenReturn(List.of(transactionTestCosto));

        given()
            .when()
            .get("/movimenti/{accountId}/transactions?from=2023-10-01&to=2023-10-31", accountId)
            .peek()
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(1))
            .body("[0].remittanceInformation", is("Test Transaction Costo"));

        verify(transactionService, times(1)).searchTransactions(
            eq(accountId),
            eq(LocalDate.parse("2023-10-01")),
            eq(LocalDate.parse("2023-10-31")),
            eq(null));
    }

    @Test
    void getTransactions_shouldReturnNotFound_whenNoTransactionIsFound() {
        when(transactionService.searchTransactions(
            eq(accountId),
            eq(LocalDate.parse("2023-10-01")),
            eq(LocalDate.parse("2023-10-31")),
            eq(null))).thenReturn(Collections.emptyList());

        given()
            .when()
            .get("/movimenti/{accountId}/transactions?from=2023-10-01&to=2023-10-31", accountId)
            .then()
            .statusCode(404)
            .body(containsString("Nessuna transazione trovata"));
    }

    @Test
    void getTransactions_shouldReturnBadRequest_whenParameterIsMissing() {
        given()
            .when()
            .get("/movimenti/{accountId}/transactions", accountId)
            .then()
            .statusCode(400)
            .body(containsString("Parametri 'from' e 'to' obbligatori"));
    }

    @Test
    void getTransactions_shouldReturnInternalServerError_whenExceptionOccurs() {
        reset(transactionService);
        when(transactionService.searchTransactions(
            eq(accountId),
            any(LocalDate.class),
            any(LocalDate.class),
            eq(null)
        )).thenThrow(new RuntimeException("Errore DB simulato"));

        given()
            .when()
            .get("/movimenti/{accountId}/transactions?from=2023-10-01&to=2023-10-31", accountId)
            .then()
            .statusCode(500)
            .body(containsString("Errore durante il recupero delle transazioni"));
    }

    @Test
    void getTransactions_shouldReturnBadRequest_whenDatesAreInverted() {
        given()
            .when()
            .get("/movimenti/{accountId}/transactions?from=2023-10-31&to=2023-10-01", accountId)
            .then()
            .statusCode(400)
            .body(containsString("La data 'from' non può essere successiva alla data 'to'."));
    }

    @Test
    void scarica_shouldReturnOk_whenSuccessful() {
        doNothing().when(transactionService).downloadAndSave(anyString(), anyString(), anyString());

        given()
            .when()
            .get("/movimenti/scarica?from=2023-10-01&to=2023-10-31")
            .then()
            .statusCode(200)
            .body("message", containsString("Movimenti scaricati e salvati"));

        verify(transactionService, times(1)).downloadAndSave(eq(accountId), eq("2023-10-01"), eq("2023-10-31"));
    }

    @Test
    void scarica_shouldReturnBadRequest_whenMissingParams() {
        given()
            .when()
            .get("/movimenti/scarica")
            .then()
            .statusCode(400)
            .body(containsString("Parametri 'from' e 'to' obbligatori"));
    }

    @Test
    void scarica_shouldReturnBadRequest_whenDatesAreInverted() {
        given()
            .when()
            .get("/movimenti/scarica?from=2023-10-31&to=2023-10-01")
            .then()
            .statusCode(400)
            .body(containsString("La data 'from' non può essere successiva alla data 'to'."));
    }

    @Test
    void classifyAndSave_shouldReturnOk_whenSuccessful() {
        CredemTransactionResponse mockResponse = new CredemTransactionResponse();
        mockResponse.booked = List.of(
            new CredemTransactionResponse.TransactionData(),
            new CredemTransactionResponse.TransactionData());

        doNothing().when(transactionService).saveTransactionsFromDTOList(any(List.class), anyString());

        given()
            .contentType(ContentType.JSON)
            .body(mockResponse)
            .when()
            .post("/movimenti/classify")
            .then()
            .statusCode(200);

        verify(transactionService, times(1)).saveTransactionsFromDTOList(any(List.class), eq(accountId));
    }

    @Test
    void classifyAndSave_shouldReturnBadRequest_whenEmptyBody() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/movimenti/classify")
            .then()
            .statusCode(400)
            .body(containsString("Il corpo della richiesta è vuoto o non contiene transazioni 'booked'"));
    }

    @Test
    void classifyAndSave_shouldReturnBadRequest_whenNullBookedTransaction() {
        CredemTransactionResponse mockResponse = new CredemTransactionResponse();
        mockResponse.booked = null;

        given()
            .contentType(ContentType.JSON)
            .body(mockResponse)
            .when()
            .post("/movimenti/classify")
            .then()
            .statusCode(400)
            .body(containsString("Il corpo della richiesta è vuoto o non contiene transazioni 'booked'."));
    }

    @Test
    void classifyAndSave_shouldReturnOk_whenEmptyBookedList() {
        CredemTransactionResponse mockResponse = new CredemTransactionResponse();
        mockResponse.booked = Collections.emptyList();

        doNothing().when(transactionService).saveTransactionsFromDTOList(any(List.class), anyString());

        given()
            .contentType(ContentType.JSON)
            .body(mockResponse)
            .when()
            .post("/movimenti/classify")
            .then()
            .statusCode(200);

        verify(transactionService, times(1)).saveTransactionsFromDTOList(any(List.class), eq(accountId));
    }

    @Test
    void ClassifyAndSave_shouldReturnBadRequest_whenBookedFieldIsMissing() {
        String requestBody = "{\"someOtherField\": \"value\", \"id\": 123}";

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/movimenti/classify")
            .then()
            .statusCode(400)
            .body(containsString("Il corpo della richiesta è vuoto o non contiene transazioni 'booked'."));
    }
    */

    // --- TEST per l'ENDPOINT GET /movimenti (l'unico attivo) ---

    @Test
    void filtraMovimenti_shouldReturnTransactions_whenOnlyAccountIdProvided() {
        // Ho cambiato accountId in "testAccountId" per coerenza con i mock di setup
        when(transactionService.searchTransactions(eq("testAccountId"), eq(null), eq(null), eq(null)))
            .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

        given()
            .queryParam("accountId", "testAccountId")
            .when()
            .get("/movimenti") // Endpoint unificato
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("[0].remittanceInformation", is("Test Transaction Costo"))
            .body("[1].remittanceInformation", is("Test Transaction Profitto"));

        verify(transactionService, times(1)).searchTransactions(eq("testAccountId"), eq(null), eq(null), eq(null));
    }
}
