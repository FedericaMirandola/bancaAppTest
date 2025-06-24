package it.coderit.banktestapp.controller;

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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.CenterType;
import it.coderit.banktestapp.service.TransactionService;

@QuarkusTest
public class TransactionControllerTest {

        @InjectMock
        TransactionService transactionService;

        @ConfigProperty(name = "credem.account-id")
        String accountId;

        // Transaction di esempio per test
        private Transaction transactionTestCosto;
        private Transaction transactionTestProfitto;

        @BeforeEach
        public void setUp() {
                // Inizializza un transaction di esempio di COSTO
                transactionTestCosto = new Transaction();
                transactionTestCosto.setId(1L);
                transactionTestCosto.setAccountId(accountId);
                transactionTestCosto.setBookingDate(OffsetDateTime.parse("2023-10-01T10:00:00Z")); // Data e ora con
                                                                                                 // offset Z (UTC)
                transactionTestCosto.setAmount(BigDecimal.valueOf(100.0));
                transactionTestCosto.setRemittanceInformation("Test Transaction Costo");
                transactionTestCosto.setCenterType(CenterType.COSTO);
                transactionTestCosto.setCurrency("EUR"); // Aggiungi la valuta

                // Inizializza un transaction di esempio di PROFITTO
                transactionTestProfitto = new Transaction();
                transactionTestProfitto.setId(2L);
                transactionTestProfitto.setAccountId(accountId);
                transactionTestProfitto.setBookingDate(OffsetDateTime.parse("2023-10-15T12:00:00Z")); // Data e ora con
                                                                                                    // offset Z (UTC)
                transactionTestProfitto.setAmount(BigDecimal.valueOf(-50.0)); // Importo negativo per profitto
                transactionTestProfitto.setRemittanceInformation("Test Transaction Profitto");
                transactionTestProfitto.setCenterType(CenterType.PROFITTO);
                transactionTestProfitto.setCurrency("EUR"); // Aggiungi la valuta

                // Configura il mock per il nuovo metodo unificato
                // Questo mock generico coprirà molti casi, ma verranno sovrascritti da mock più
                // specifici
                when(transactionService.searchTransactions(anyString(), any(LocalDate.class), any(LocalDate.class),
                                any(CenterType.class)))
                                .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

                when(transactionService.searchTransactions(eq(accountId), any(LocalDate.class), any(LocalDate.class),
                                eq(CenterType.COSTO)))
                                .thenReturn(List.of(transactionTestCosto));

                when(transactionService.searchTransactions(eq(accountId), any(LocalDate.class), any(LocalDate.class),
                                eq(CenterType.PROFITTO)))
                                .thenReturn(List.of(transactionTestProfitto));

                // Mock per il caso in cui vengano richieste solo transazioni per un accountId
                // senza altri filtri
                when(transactionService.searchTransactions(eq(accountId), eq(null), eq(null), eq(null)))
                                .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

                // Mock del metodo downloadAndSave
                doNothing().when(transactionService).downloadAndSave(anyString(), anyString(), anyString());

                // Mock del metodo saveTransactionsFromDTOList con il defaultAccountId
                doNothing().when(transactionService).saveTransactionsFromDTOList(any(List.class), anyString());
        }

        // --- Test per GET /movimenti/{accountId}/transactions (Aggiornato per usare
        // searchTransactions) ---

        @Test
        void getTransactions_shouldReturnTransactions_ifFound() {
                // Ora il service legge tramite searchTransactions.
                // Simuliamo che, con quelle date, venga trovato il transaction di costo.
                when(transactionService.searchTransactions(
                                eq(accountId),
                                eq(LocalDate.parse("2023-10-01")),
                                eq(LocalDate.parse("2023-10-31")),
                                eq(null) // Nessun centerType per questo endpoint
                )).thenReturn(List.of(transactionTestCosto));

                given()
                                .when()
                                .get("/movimenti/{accountId}/transactions?from=2023-10-01&to=2023-10-31", accountId)
                                .peek() // Per vedere la risposta nel log, utile in debug
                                .then()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("size()", is(1))
                                .body("[0].remittanceInformation", is("Test Transaction Costo"));

                // Verifica che il service sia stato chiamato correttamente
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
                // Mock specifico per questo test che forza l'eccezione
                when(transactionService.searchTransactions(
                                eq(accountId), // accountId
                                any(LocalDate.class), // fromDateObj
                                any(LocalDate.class), // toDateObj
                                eq(null) // centerType
                )).thenThrow(new RuntimeException("Errore DB simulato"));

                given()
                                .when()
                                .get("/movimenti/{accountId}/transactions?from=2023-10-01&to=2023-10-31", accountId)
                                .then()
                                .statusCode(500)
                                .body(containsString("Errore durante il recupero delle transazioni"));

                verify(transactionService, times(1)).searchTransactions(
                                eq(accountId),
                                any(LocalDate.class),
                                any(LocalDate.class),
                                eq(null));
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

        // --- Test per GET /movimenti/scarica (Endpoint dedicato, non è cambiato) ---

        @Test
        void scarica_shouldReturnOk_whenSuccessful() {
                doNothing().when(transactionService).downloadAndSave(anyString(), anyString(), anyString());

                given()
                                .when()
                                .get("/movimenti/scarica?from=2023-10-01&to=2023-10-31")
                                .then()
                                .statusCode(200)
                                .body("message", containsString("Movimenti scaricati e salvati"));

                verify(transactionService, times(1)).downloadAndSave(eq(accountId), eq("2023-10-01"),
                                eq("2023-10-31"));
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

        // --- Test per POST /movimenti/classify (Path corretto) ---

        @Test
        void classifyAndSave_shouldReturnOk_whenSuccessful() {
                CredemTransactionResponse mockResponse = new CredemTransactionResponse();
                mockResponse.booked = List.of(
                                new CredemTransactionResponse.TransactionData(),
                                new CredemTransactionResponse.TransactionData());

                // Modifica del mock per includere defaultAccountId
                doNothing().when(transactionService).saveTransactionsFromDTOList(any(List.class), anyString());

                given()
                                .contentType(ContentType.JSON)
                                .body(mockResponse)
                                .when()
                                .post("/movimenti/classify") // Path corretto
                                .then()
                                .statusCode(200);

                // Verifica che il service sia stato chiamato con la lista e l'accountId
                verify(transactionService, times(1)).saveTransactionsFromDTOList(any(List.class), eq(accountId));
        }

        @Test
        void classifyAndSave_shouldReturnBadRequest_whenEmptyBody() {
                given()
                                .contentType(ContentType.JSON)
                                .body("{}")
                                .when()
                                .post("/movimenti/classify") // Path corretto
                                .then()
                                .statusCode(400)
                                .body(containsString(
                                                "Il corpo della richiesta è vuoto o non contiene transazioni 'booked'"));
        }

        @Test
        void classifyAndSave_shouldReturnBadRequest_whenNullBookedTransaction() {
                CredemTransactionResponse mockResponse = new CredemTransactionResponse();
                mockResponse.booked = null;

                given()
                                .contentType(ContentType.JSON)
                                .body(mockResponse)
                                .when()
                                .post("/movimenti/classify") // Path corretto
                                .then()
                                .statusCode(400)
                                .body(containsString(
                                                "Il corpo della richiesta è vuoto o non contiene transazioni 'booked'"));
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
                                .post("/movimenti/classify") // Path corretto
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
                                .post("/movimenti/classify") // Path corretto
                                .then()
                                .statusCode(400)
                                .body(containsString(
                                                "Il corpo della richiesta è vuoto o non contiene transazioni 'booked'."));
        }

        // --- NUOVI TEST PER L'ENDPOINT /movimenti/filtra ---

        @Test
        void filtraMovimenti_shouldReturnTransactions_whenOnlyAccountIdProvided() {
                // Mock specifico per il caso senza date né centerType
                when(transactionService.searchTransactions(eq(accountId), eq(null), eq(null), eq(null)))
                                .thenReturn(List.of(transactionTestCosto, transactionTestProfitto));

                given()
                                .queryParam("accountId", accountId)
                                .when()
                                .get("/movimenti/filtra")
                                .peek()
                                .then()
                                .statusCode(200)
                                .contentType(ContentType.JSON)
                                .body("size()", is(2))
                                .body("[0].remittanceInformation", is("Test Transaction Costo"))
                                .body("[1].remittanceInformation", is("Test Transaction Profitto"));

                verify(transactionService, times(1)).searchTransactions(eq(accountId), eq(null), eq(null), eq(null));
        }
}