package it.coderit.banktestapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import io.restassured.http.ContentType;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.model.Movimento;
import it.coderit.banktestapp.model.TipoCentro;
import it.coderit.banktestapp.service.MovimentoService;

@QuarkusTest
public class MovimentoControllerTest {

    @InjectMock
    MovimentoService movimentoService;

    @ConfigProperty(name = "credem.account-id")
    String accountId;

    // Movimento di esempio per test
    private Movimento movimentoTest;

    @BeforeEach
    public void setUp() {
        // Inizializza un movimento di esempio
        movimentoTest = new Movimento();
        movimentoTest.setId(1L);
        movimentoTest.setAccountId(accountId);
        movimentoTest.setBookingDate(java.time.OffsetDateTime.parse("2023-10-01T00:00:00+00:00"));
        movimentoTest.setAmount(java.math.BigDecimal.valueOf(100.0));
        movimentoTest.setRemittanceInformation("Test Transaction");
        movimentoTest.setTipoCentro(TipoCentro.COSTO);

        // Configura il mock per restituire il movimento di test
        when(movimentoService.leggiMovimentiPerAccountIdEData(anyString(), anyString(), anyString()))
                .thenReturn(List.of(movimentoTest));
    }

    // ------Test per GET /movimenti/{accountId}/transactions------

    // comportamento atteso quando si effettua una GET per recuperare le transazioni
    // e queste vengono trovate nel DB
    @Test
    void getTransactions_shouldreturnTransactions_ifFound() {
        when(movimentoService.leggiMovimentiPerAccountIdEData(accountId, "2023-10-01", "2023-10-31"))
                .thenReturn(List.of(movimentoTest));

        given()
                .when()
                .get("/movimenti/{accountId}/transactions?from=2023-10-01&to=2023-10-31", accountId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(1))
                .body("[0].description", is("pagamento affitto"));
    }

    // comporamento atteso quando si effettua una GET ma non ci sono transazioni
    @Test
    void getTransactions_shouldReturnNotFound_whenNoTransactionIsFound() {
        when(movimentoService.leggiMovimentiPerAccountIdEData(accountId, "2023-10-01", "2023-10-31"))
                .thenReturn(Collections.emptyList());

        given()
                .when()
                .get("/movimenti/{accountId}/transactions?from=2023-10-01&to=2023-10-31", accountId)
                .then()
                .statusCode(404)
                .body(containsString("Nessuna transazione trovata"));
    }

    // comportamento atteso quando si effettua una chiamata GET
    // ma i parametri 'from' e 'to' sono mancanti
    @Test
    void getTransactions_shouldReturnBadRequest_whenParameterIsMissing() {
        given()
                .when()
                .get("/movimenti/{accountId}/transactions", accountId)
                .then()
                .statusCode(400)
                .body(containsString("Parametri 'from' e 'to' obbligatori"));

    }

    // Comportamento atteso quando si effettua una GET ma i parametri
    // 'from' e 'to' non sono validi (es. formato data errato)
    @Test
    void getTransactions_shouldReturnInternalServerError_whenExceptionOccurs() {
        when(movimentoService.leggiMovimentiPerAccountIdEData(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Errore DB simulato"));

        given()
                .when()
                .get("/movimenti/{accountId}/transactions?from=2023-10-01&to=2023-10-31", accountId)
                .then()
                .statusCode(500)
                .body(containsString("Errore durante il recupero delle transazioni"));
    }

    // Comportamento atteso quando si effettua una GET ma i parametri from e to
    // sono invertiti
    @Test
    void getTransactions_shouldReturnBadRequest_whenDatesAreInverted() {
        given()
                .when()
                .get("/movimenti/{accountId}/transactions?from=2023-10-31&to=2023-10-01", accountId)
                .then()
                .statusCode(400)
                .body(containsString("La data 'from' non può essere dopo la data 'to'."));
    }

    // ------Test per GET /movimenti/scarica------

    // Comportamento atteso quando si effettua una GET per scaricare i movimenti
    // e questi vengono scaricati e memorizzati correttamente
    @Test
    void scarica_shouldReturnOk_whenSuccessful() {
        // Simula il comportamento del servizio per il download dei movimenti
        doNothing().when(movimentoService).scaricaEMemorizzaMovimenti(anyString(), anyString(), anyString());

        given()
                .when()
                .get("/movimenti/scarica?from=2023-10-01&to=2023-10-31")
                .then()
                .statusCode(200)
                .body("message", containsString("Movimenti scaricati e salvati"));
    }

    // Comportamento atteso quando si effettua una GET ma i parametri mancano
    @Test
    void scarica_shouldReturnBadRequest_whenMissingParams() {
        given()
                .when()
                .get("/movimenti/scarica")
                .then()
                .statusCode(400)
                .body(containsString("Parametri 'from' e 'to' obbligatori"));
    }

    // Comportamento atteso quando si effettua una GET ma i parametri from e to
    // sono invertiti
    @Test
    void scarica_shouldreturnBadrequest_whenDatesAreInverted() {
        given()
                .when()
                .get("/movimenti/scarica?from=2023-10-31&to=2023-10-01")
                .then()
                .statusCode(400)
                .body(containsString("La data 'from' non può essere dopo la data 'to'."));
    }

    // ------Test per POST /movimenti/classify-and-save------

    // Comportamento atteso quando si effettua una POST per classificare e salvare i
    // movimenti
    @Test
    void classifyAndSave_shouldReturnOk_whenSuccessful() {
        CredemTransactionResponse mockResponse = new CredemTransactionResponse();
        mockResponse.booked = Arrays.asList(
                new CredemTransactionResponse.TransactionData(),
                new CredemTransactionResponse.TransactionData());

        doNothing().when(movimentoService).salvaMovimentiDaLista(any(List.class));
        given()
                .contentType(ContentType.JSON)
                .body(mockResponse)
                .when()
                .post("/movimenti/classify-and-save")
                .then()
                .statusCode(200);

        Mockito.verify(movimentoService, Mockito.times(1)).salvaMovimentiDaLista(any(List.class));

    }

    // Comportamento atteso quando si effettua una POST ma il corpo della richiesta
    // è vuoto
    @Test
    void classifyAndSave_shouldReturnBadRequest_whenEmptyBody() {
        given()
                .contentType(ContentType.JSON)
                .body("{}") // Corpo vuoto
                .when()
                .post("/movimenti/classify-and-save")
                .then()
                .statusCode(400)
                .body(containsString("Il corpo della richiesta è vuoto o non contiene transazioni 'booked'"));
    }

    // Comportamento atteso quando si effettua una POST ma il corpo della richiesta
    // non contiene transazioni nel nodo 'booked'
    @Test
    void classifyAndSave_shouldReturnBadRequest_whenNullBookedTransaction() {
        CredemTransactionResponse mockResponse = new CredemTransactionResponse();
        mockResponse.booked = null; // campo booked nullo

        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/movimenti/classify-and-save")
                .then()
                .statusCode(400)
                .body(containsString("Il corpo della richiesta è vuoto o non contiene transazioni 'booked'"));
    }

    // Comportamento atteso quando si effettua una POST per classificare e salvare i
    // movimenti ma la lista 'booked' è vuota. il controller non rifiuta una lista
    // vuota
    @Test
    void classifyAndSave_shouldReturnOk_whenEmptyBookedList() {
        CredemTransactionResponse mockResponse = new CredemTransactionResponse();
        // lista 'booked' è lista vuota, non null
        mockResponse.booked = Collections.emptyList();

        // Mock del service per non fare nulla quando chiamato con una lista vuota
        doNothing().when(movimentoService).salvaMovimentiDaLista(any(List.class));

        given()
                .contentType(ContentType.JSON)
                .body(mockResponse) // Il corpo JSON sarà {"booked": []}
                .when()
                .post("/movimenti/classify-and-save")
                .then()
                .statusCode(200); // Ci aspettiamo 200 OK

        // Verifichiamo che il metodo del servizio sia stato chiamato esattamente una
        // volta
        // anche con una lista vuota, perché il controller lo delega comunque al
        // service.
        Mockito.verify(movimentoService, Mockito.times(1)).salvaMovimentiDaLista(any(List.class));
    }

    // Comportamento atteso quando si effettua una POST per classificare e salvare i
    // movimenti ma il corpo della richiesta non contiene il campo 'booked'
    @Test
    void ClassifyAndSave_shouldReturnBadRequest_whenBookedFieldIsMissing() {
        // Creiamo un JSON che non contiene il campo "booked"
        String requestBody = "{\"someOtherField\": \"value\", \"id\": 123}";

        given()
                .contentType(ContentType.JSON)
                .body(requestBody) // Il corpo JSON non ha "booked"
                .when()
                .post("/movimenti/classify-and-save")
                .then()
                .statusCode(400) // Ci aspettiamo 400 Bad Request
                .body(containsString("Il corpo della richiesta è vuoto o non contiene transazioni 'booked'."));
    }

    // ------Test per GET /movimenti/by-centro------

    // Comportamento atteso quando si effettua una GET per recuperare i movimenti
    // per tipo centro e il tipo centro è valido
    @Test
    void byCentro_shouldReturnMovimenti_whenTipoCentroIsValid() {
        List<Movimento> mockMovimenti = Arrays.asList(movimentoTest);
        when(movimentoService.leggiMovimentiPerTipoCentro(TipoCentro.COSTO))
                .thenReturn(mockMovimenti);

        given()
                .queryParam("tipo", "COSTO")
                .when()
                .get("/movimenti/by-centro")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].tipoCentro", is("COSTO"));
    }

    // Comportamento atteso quando si effettua una GET per recuperare i movimenti
    // per tipo centro ma il tipo centro è lasciato vuoto
    @Test
    void byCentro_shouldReturnBadRequest_whenTipoCentroIsMissing() {
        given()
                .when()
                .get("/movimenti/by-centro")
                .then()
                .statusCode(400)
                .body(containsString("Parametro 'tipo' obbligatorio"));
    }

    // Comportamento atteso quando si effettua una GET per recuperare i movimenti
    // per tipo centro ma il tipo centro non è valido (es. "INVALIDO")
    @Test
    void byCentro_shouldReturnBadRequest_whenParamIsInvalid() {
        given()
                .when()
                .get("/movimenti/by-centro?tipo=INVALIDO")
                .then()
                .statusCode(400)
                .body(containsString("Tipo centro non valido, valori possibili: COSTO, PROFITTO"));
    }

    // Comportamento atteso quando si effettua una GET per recuperare i movimenti
    // per tipo centro ma non ci sono movimenti per quel tipo centro
    @Test
    void byCentro_shouldReturnNotFound_whenMovimentiNotFound() {
        when(movimentoService.leggiMovimentiPerTipoCentro(TipoCentro.COSTO))
                .thenReturn(Collections.emptyList());

        given()
                .when()
                .get("/movimenti/by-centro?tipo=COSTO")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(0));
    }

    // Comportamento atteso quando si effettua una GET per recuperare i movimenti
    // per tipo centro ma il tipo centro è passato in minuscolo (es. "costo")
    @Test
    void byCentro_shouldReturnMovimenti_whenTipoCentroIsValidCaseInsensitive() {
        // Mock del service per restituire un movimento quando chiamato con
        // TipoCentro.COSTO
        // Questo simula che il service riceva correttamente l'enum COSTO
        when(movimentoService.leggiMovimentiPerTipoCentro(TipoCentro.COSTO))
                .thenReturn(Arrays.asList(movimentoTest));

        given()
                .when()
                .get("/movimenti/by-centro?tipo=costo") // Chiamiamo con "costo" minuscolo
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(1))
                .body("[0].tipoCentro", is("COSTO")); // Verifichiamo che il TipoCentro nel JSON sia in maiuscolo (come
                                                      // nell'enum)
    }

    // Comportamento atteso quando si effettua una GET per recuperare i movimenti
    // per tipo centro ma il tipo centro è valido ma non ci sono movimenti
    @Test
    void byCentro_shouldReturnEmptyList_whenNoMovimentiFoundForValidType() {
        // Mockiamo il service per restituire una lista vuota quando chiamato con
        // TipoCentro.PROFITTO
        when(movimentoService.leggiMovimentiPerTipoCentro(TipoCentro.PROFITTO))
                .thenReturn(Collections.emptyList());

        given()
                .when()
                .get("/movimenti/by-centro?tipo=PROFITTO") // Tipo valido, ma senza movimenti
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(0)); // Ci aspettiamo una lista vuota
    }

}
