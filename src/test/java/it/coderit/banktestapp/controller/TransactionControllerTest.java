package it.coderit.banktestapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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
import java.util.UUID;

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

    private final String DEFAULT_TEST_ACCOUNT_ID = "IT001000000000000000001";

    private Transaction transactionTestCosto;
    private Transaction transactionTestProfitto;
    private Transaction transactionTestCosto2;

    @BeforeEach
    public void setUp() {
        Mockito.reset(transactionService);

        transactionTestCosto = new Transaction();
        transactionTestCosto.setTransactionId(UUID.randomUUID().toString());
        transactionTestCosto.setAccountId(DEFAULT_TEST_ACCOUNT_ID);
        transactionTestCosto.setBookingDate(OffsetDateTime.parse("2023-10-01T10:00:00Z"));
        transactionTestCosto.setAmount(BigDecimal.valueOf(-50.0));
        transactionTestCosto.setRemittanceInformation("Test Transaction Costo");
        transactionTestCosto.setCenterType(CenterType.COSTO);
        transactionTestCosto.setCurrency("EUR");

        transactionTestProfitto = new Transaction();
        transactionTestProfitto.setTransactionId(UUID.randomUUID().toString());
        transactionTestProfitto.setAccountId(DEFAULT_TEST_ACCOUNT_ID);
        transactionTestProfitto.setBookingDate(OffsetDateTime.parse("2023-10-15T12:00:00Z"));
        transactionTestProfitto.setAmount(BigDecimal.valueOf(100.0));
        transactionTestProfitto.setRemittanceInformation("Test Transaction Profitto");
        transactionTestProfitto.setCenterType(CenterType.PROFITTO);
        transactionTestProfitto.setCurrency("EUR");

        transactionTestCosto2 = new Transaction();
        transactionTestCosto2.setTransactionId(UUID.randomUUID().toString());
        transactionTestCosto2.setAccountId(DEFAULT_TEST_ACCOUNT_ID);
        transactionTestCosto2.setBookingDate(OffsetDateTime.parse("2023-10-15T12:00:00Z"));
        transactionTestCosto2.setRemittanceInformation("Altro costo");
        transactionTestCosto2.setCenterType(CenterType.COSTO);
        transactionTestCosto2.setCurrency("EUR");

    }

     // --- TEST per l'ENDPOINT GET /transactions ---


    //deve restituire tutte le transazoni quando non sono formiti parametri
    @Test
    void transactions_shouldReturnTransactions_whenNoQueryParams() {
        
        when(transactionService.searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null), eq(null)))
            .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

        given()
            .auth().basic("user", "userpassword")
            .when()
                .get("/transactions") 
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(2))
                .body("[0].remittanceInformation", is(transactionTestCosto.getRemittanceInformation()))
                .body("[1].remittanceInformation", is(transactionTestProfitto.getRemittanceInformation()));

        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null), eq(null));
    }

   //deve ritornare una lista di transazioni quando un range di date è formito
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
            
            verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(fromDate), eq(toDate), eq(null));
   }

   //deve restituire una lista di transazioni quando un "Tipo Centro" è fornito
   @Test
   void transactions_shouldReturnFilteredTransactions_whenCenterTypeIsProvided() {
        when(transactionService.searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null), eq(CenterType.COSTO)))
            .thenReturn(List.of(transactionTestCosto, transactionTestCosto2));
        
        given()
        .auth().basic("user", "userpassword")
        .queryParam("centerType", "COSTO")
        .when()
            .get("/transactions")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("[0].centerType", is(CenterType.COSTO.name()))
            .body("[1].centerType", is(CenterType.COSTO.name()));
        
        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null), eq(CenterType.COSTO));

   }

   //deve ritornare una lista di transazioni quando range di date e tipo Centro sono entrambi forniti
   @Test
   void transactions_shouldReturnFilteredTransactions_whenCenterTypeAndDatesAreProvided() {
        LocalDate fromDate = LocalDate.of(2023, 1, 1);
        LocalDate toDate = LocalDate.of(2023, 1, 31);

        when(transactionService.searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(fromDate), eq(toDate), eq(CenterType.PROFITTO)))
            .thenReturn(List.of(transactionTestProfitto));
        
        given()
            .auth().basic("user", "userpassword")
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

        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(fromDate), eq(toDate), eq(CenterType.PROFITTO));
   }
   
   //deve restituire una lista vuota quando non trova nessuna transazione
   @Test
   void transactions_shouldReturnEmptyList_whenNoTransactionIsFound() {
        when(transactionService.searchTransactions(any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());
        
            given()
            .auth().basic("user", "userpassword")
            .when()
                .get("/transactions")
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(0))
                .body("", is(empty()));

        verify(transactionService, times(1)).searchTransactions(eq(DEFAULT_TEST_ACCOUNT_ID), eq(null), eq(null), eq(null));
   }

   // deve restituire errore 400 quando viene fornita solo la data From
   @Test
   void transactions_shouldReturnBadRequest_whenOnlyDateFromIsProvided() {
        given()
        .auth().basic("user", "userpassword")
        .queryParam("from", "2023-01-01")
        .when()
            .get("/transactions")
        .then()
            .statusCode(400)
            .body(is("Per il filtro per data, entrambi i parametri 'from' e 'to' sono obbligatori."));
        
        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());
   }

   //deve restituire errore 400 quando viene fornita solo la data To
   @Test
   void transactions_shouldReturnBadrequest_whenOnlyDateToIsProvided() {
        given()
        .auth().basic("user", "userpassword")
        .queryParam("to", "2023-01-31")
        .when()
            .get("/transactions")
        .then()
            .statusCode(400)
            .body(is("Per il filtro per data, entrambi i parametri 'from' e 'to' sono obbligatori."));

        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());

   }

   //deve restituire errore 400 quando il fomato data non è valido
   @Test
   void transactions_shouldReturnbadRequest_whenDateFormatInvalid() {
        given()
        .auth().basic("user", "userpassword")
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
        .auth().basic("user", "userpassword")
        .queryParam("from", "2023-01-31")
        .queryParam("to", "2023-01-01")
        .when()
            .get("/transactions")
        .then()
            .statusCode(400)
            .body(is("La data 'from' non può essere successiva alla data 'to'."));
        
        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());
   }

   //deve restituire errore 400 quando il tipo Centro non è valido
   @Test
   void transactions_shouldReturnBadRequest_whenCenterTypeIsInvalid() {
        given()
        .auth().basic("user", "userpassword")
        .queryParam("centerType", "INVALID_TYPE")
        .when()
            .get("/transactions")
        .then()
            .statusCode(400)
            .body(is("Tipo center non valido. Valori possibili: COSTO, PROFITTO"));
        
        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());
   }

   //deve retituire errore 500 quando viene lanciata un eccezione
   @Test
   void transactions_shouldreturnInternalServerError_whenServiceThrowException() {
        when(transactionService.searchTransactions(any(), any(), any(), any())).thenThrow
            (new RuntimeException("Errore server simulato"));

        given()
        .auth().basic("user", "userpassword")
        .when()
            .get("/transactions")
        .then()
            .statusCode(500)
            .body(is("Errore durante il recupero/filtro dei movimenti: Errore server simulato"));
   }

   // deve restituire errore 401 non autorizzato quando non si effettua l'autenticazione
   @Test
   void transactions_shouldReturnUnathorized_whenNotAuthenticated() {
        given()
        .when()
            .get("/transactions")
        .then()
            .statusCode(401);
        
        verify(transactionService, never()).searchTransactions(any(), any(), any(), any());
   }
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

    // --- TEST per l'ENDPOINT GET /transactions ---

    
}
