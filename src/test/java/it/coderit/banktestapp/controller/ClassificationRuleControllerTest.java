/*package it.coderit.banktestapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.restassured.http.ContentType;

// import groovy.xml.Entity; // Non usato, può essere rimosso
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.coderit.banktestapp.dto.RuleInput;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.model.CenterType;
import it.coderit.banktestapp.repository.ClassificationRuleRepository;
import jakarta.persistence.EntityManager; // Non usato direttamente, ma può essere mantenuto per InjectMock

@QuarkusTest
public class ClassificationRuleControllerTest {
    
    @InjectMock
    ClassificationRuleRepository regolaRepository;

    @InjectMock
    EntityManager entityManager; // Mantenuto per coerenza con la tua struttura, anche se non usato direttamente nei test

    private ClassificationRule regola1;
    private ClassificationRule regola2;

    @BeforeEach
    void setUp() {
        // si resettano i mock prima di ogni test
        Mockito.reset(regolaRepository, entityManager);

        //inizializzazione rules esempio per test
        regola1 = new ClassificationRule();
        regola1.setId(1L);
        regola1.setKeyword("affitto");
        regola1.setCenter(CenterType.COSTO);
        // HO CAMBIATO QUI: "casa" in minuscolo per corrispondere all'output effettivo
        regola1.setJsonRule("{\"categoria\": \"casa\", \"sottocategoria\": \"Affitto\"}");

        regola2 = new ClassificationRule();
        regola2.setId(2L);  
        regola2.setKeyword("stipendio");
        regola2.setCenter(CenterType.PROFITTO);
        regola2.setJsonRule(null);

        when(regolaRepository.listAll()).thenReturn(List.of(regola1, regola2));
        
    }

    // -------- TEST per GET /rules --------

    // Test per ottenere la lista delle rules
    @Test
    void rules_shouldReturnAllRules() {
        given()
            .when()
                .get("/rules")
            .then()
                .statusCode(200)
                .body("size()", is(2))
                .body("[0].id", is(1))
                .body("[0].keyword", equalTo("affitto"))
                .body("[0].center", equalTo(CenterType.COSTO.name()))
                .body("[1].id", is(2))
                .body("[1].keyword", equalTo("stipendio"))
                .body("[1].center", equalTo(CenterType.PROFITTO.name()));
        
        //verifica che il metodo del repository sia stato chiamato
        verify(regolaRepository, times(1)).listAll();

    }

    @Test
    void rules_shouldReturnEmptyList_whenNoRuleExists() {
        // sovrascive il mock di default
        when(regolaRepository.listAll()).thenReturn(Collections.emptyList());

        given()
            .when()
                .get("/rules")
            .then()
                .statusCode(200)
                .body("size()", is(0));
        
        //verifica che il metodo del repository sia stato chiamato
        verify(regolaRepository, times(1)).listAll();
    }

    // -------- TEST per POST /rules/add --------

    @Test
    void addRule_shouldCreateNewRule_whenValidInput() {
        // Mock del comportamento di persist
        doNothing().when(regolaRepository).persist(any(ClassificationRule.class));

        RuleInput input1 = new RuleInput();
        input1.keyword = "benzina";
        input1.center = CenterType.COSTO;
        input1.jsonRule = "{\"dettaglio\": \"spese auto\"}";

        RuleInput input2 = new RuleInput();
        input2.keyword = "interessi";
        input2.center = CenterType.PROFITTO;
        input2.jsonRule = null;

        List<RuleInput> rulesToCreate = Arrays.asList(input1, input2);

        given()
            .contentType(ContentType.JSON)
            .body(rulesToCreate)
            .when()
                .post("/rules/add")
            .then()
                .statusCode(200)
                .body(equalTo("Nuove rules create con successo!"));
        
        //verifica del persist per ogni regola
        verify(regolaRepository, times(2))
        .persist(any(ClassificationRule.class));
    }

    @Test
    void addRule_shouldReturnBadRequest_whenEmptyInput() {
        List<RuleInput> emptyList = Collections.emptyList();

        given() 
            .contentType(ContentType.JSON)
            .body(emptyList)
            .when()
                .post("/rules/add")
            .then()
                .statusCode(400)
                .body(equalTo("I campi non possono essere vuoti"));
        
        //verifica che nn abbia fatto persist
        verify(regolaRepository, never()).persist(any(ClassificationRule.class));
    }

    @Test
    void add_shouldReturnBadRequest_whenInputIsNull() {
        given()
            .contentType(ContentType.JSON)
            .body("null")
            .when()
                .post("/rules/add")
            .then()
                .statusCode(400)
                .body(equalTo("I campi non possono essere vuoti"));
        
                verify(regolaRepository, never()).persist(any(ClassificationRule.class));
    }

    @Test
    void addRule_shouldReturnBadRequest_whenMissingKeyword() {
        RuleInput input = new RuleInput();
        input.keyword = null;
        input.center = CenterType.COSTO;

        given()
            .contentType(ContentType.JSON)
            .body(Collections.singletonList(input))
            .when()
                .post("/rules/add")
            .then()
                .statusCode(400)
                .body(equalTo("keyword e center sono campi obbligatori"));
        
        verify(regolaRepository, never()).persist(any(ClassificationRule.class));
    }

    @Test
    void addRule_shouldReturnBadRequest_whenCentroIsMissing() {
        RuleInput input = new RuleInput();
        input.keyword = "test";
        input.center = null;

        given()
            .contentType(ContentType.JSON)
            .body(Collections.singletonList(input))
            .when()
                .post("/rules/add")
            .then()
                .statusCode(400)
                .body(equalTo("keyword e center sono campi obbligatori"));
        
        verify(regolaRepository, never()).persist(any(ClassificationRule.class));
    }

    @Test
    void addRule_ShouldReturnInternalServerError_whenPersistFails() {
        //simula errore durante la persistenza
        doThrow(new RuntimeException("Errore DB simulato"))
            .when(regolaRepository).persist(any(ClassificationRule.class));
        
            RuleInput input = new RuleInput();
        input.keyword = "errore";
        input.center = CenterType.COSTO;

        given()
                .contentType(ContentType.JSON)
                .body(Collections.singletonList(input))
                .when()
                    .post("/rules/add")
                .then()
                    .statusCode(500)
                    .body(containsString("Errore DB simulato"));

        verify(regolaRepository, times(1)).persist(any(ClassificationRule.class));
    }
    
    // ------- TEST per PUT /rules/{id} --------

    @Test
    void update_shouldUpdateExistingRule_whenValidInput() {
        Long existingId = 1L;
        // Mockiamo findById per restituire una regola esistente
        when(regolaRepository.findById(existingId)).thenReturn(regola1);
        

        RuleInput updatedInput = new RuleInput();
        updatedInput.keyword = "affitto_aggiornato";
        updatedInput.center = CenterType.PROFITTO;
        updatedInput.jsonRule = "{\"nuovo\": \"json\"}";

        given()
                .contentType(ContentType.JSON)
                .body(updatedInput)
                .when()
                    .put("/rules/{id}", existingId)
                .then()
                    .statusCode(200)
                    .body("id", is(existingId.intValue()))
                    .body("keyword", equalTo("affitto_aggiornato"))
                    .body("center", equalTo(CenterType.PROFITTO.name()))
                    .body("jsonRule", equalTo("{\"nuovo\": \"json\"}"));

        // Verifica che findById sia stato chiamato e che i setters siano stati usati sull'oggetto mockato
        verify(regolaRepository, times(1)).findById(existingId);
        
    }

    @Test
    void update_shouldReturnNotFound_whenRuleDoesNotExist() {
        Long nonExistentId = 99L;
        when(regolaRepository.findById(nonExistentId)).thenReturn(null); // Rule non trovata

        RuleInput input = new RuleInput();
        input.keyword = "qualsiasi";
        input.center = CenterType.COSTO;

        given()
                .contentType(ContentType.JSON)
                .body(input)
                .when()
                    .put("/rules/{id}", nonExistentId)
                .then()
                    .statusCode(404)
                    .body(equalTo("Rule non trovata"));

        verify(regolaRepository, times(1)).findById(nonExistentId);
    }
    
    @Test
    void update_shouldUpdatePartially_whenPartialInput() {
        Long existingId = 1L;
        // Mockiamo findById per restituire una regola esistente
        when(regolaRepository.findById(existingId)).thenReturn(regola1); // Usiamo regola1 come base

        RuleInput partialInput = new RuleInput();
        partialInput.keyword = "solo_parola"; // Aggiorno solo la parola chiave
        partialInput.center = null; 
        partialInput.jsonRule = null; 

        given()
                .contentType(ContentType.JSON)
                .body(partialInput)
                .when()
                    .put("/rules/{id}", existingId)
                .then()
                    .statusCode(200)
                    .body("id", is(existingId.intValue()))
                    .body("keyword", equalTo("solo_parola"))
                    .body("center", equalTo(CenterType.COSTO.name())) 
                    .body("jsonRule", equalTo("{\"categoria\": \"casa\", \"sottocategoria\": \"Affitto\"}")); 

        verify(regolaRepository, times(1)).findById(existingId);
    }


    @Test
    void update_shouldReturnInternalServerError_whenUpdateFails() {
        Long existingId = 1L;
        // Simula un errore nel trovare la regola
        
        when(regolaRepository.findById(existingId)).thenThrow(new RuntimeException("Errore DB durante findById"));

        RuleInput input = new RuleInput();
        input.keyword = "qualsiasi";
        input.center = CenterType.COSTO;

        given()
                .contentType(ContentType.JSON)
                .body(input)
                .when()
                    .put("/rules/{id}", existingId)
                .then()
                    .statusCode(500)
                    .body(containsString("Errore DB durante findById")); // Assicurati che il messaggio di errore sia quello atteso

        verify(regolaRepository, times(1)).findById(existingId);
    }

    
    //----------- Test per DELETE /rules/{id} -------------

    @Test
    void delete_shouldDeleteRule_whenRuleExists() {
        Long existingId = 1L;
        when(regolaRepository.deleteById(existingId)).thenReturn(true); // Simula la cancellazione riuscita

        given()
                .when()
                .delete("/rules/{id}", existingId)
                .then()
                .statusCode(204); // No Content

        verify(regolaRepository, times(1)).deleteById(existingId);
    }

    @Test
    void delete_shouldReturnNotFound_whenRuleDoesNotExist() {
        Long nonExistentId = 99L;
        when(regolaRepository.deleteById(nonExistentId)).thenReturn(false); // Simula la cancellazione fallita (non trovata)

        given()
                .when()
                .delete("/rules/{id}", nonExistentId)
                .then()
                .statusCode(404)
                .body(equalTo("Rule non trovata"));

        verify(regolaRepository, times(1)).deleteById(nonExistentId);
    }

    @Test
    void delete_shouldReturnInternalServerError_whenDeleteFails() {
        Long existingId = 1L;
        // Simula un errore durante la cancellazione (es. problema di DB)
        doThrow(new RuntimeException("Errore DB durante delete")).when(regolaRepository).deleteById(existingId);

        given()
                .when()
                .delete("/rules/{id}", existingId)
                .then()
                .statusCode(500)
                .body(containsString("Errore DB durante delete"));

        verify(regolaRepository, times(1)).deleteById(existingId);
    }
}
*/