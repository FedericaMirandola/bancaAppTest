package it.coderit.banktestapp.controller;

import it.coderit.banktestapp.dto.RuleInput;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.model.CenterType;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.repository.ClassificationRuleRepository;
import it.coderit.banktestapp.repository.TransactionRepository;
import it.coderit.banktestapp.service.RuleEngineService;
import it.coderit.banktestapp.service.TransactionService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClassificationRuleController {

    private static final Logger log = LoggerFactory.getLogger(ClassificationRuleController.class);

    @Inject
    TransactionService transactionService; 

    @Inject
    ClassificationRuleRepository ruleRepository;

    @Inject
    TransactionRepository transactionRepository; 

    @Inject
    RuleEngineService ruleEngineService; 

    
    //Restituisce tutte le regole di classificazione presenti nel database.
     
    @GET
    public List<ClassificationRule> getAllRules() {
        log.info("Richiesta di tutte le regole di classificazione.");
        return ruleRepository.listAll();
    }

    // Restituisce una regola di classificazione per il suo ID.
    @GET
    @Path("/{id}")
    public Response getRuleById(@PathParam("id") Long id) {
        log.info("Richiesta regola di classificazione con ID: {}", id);
        return ruleRepository.findByIdOptional(id)
                .map(rule -> Response.ok(rule).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    // Crea una nuova regola di classificazione.
    @POST
    @Transactional
    @Path("/add")
    public Response createRule(RuleInput ruleInput) {
        log.info("Ricevuta richiesta di creazione regola: {}", ruleInput);

        if (ruleInput.getKeyword() == null || ruleInput.getKeyword().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("La parola chiave non può essere vuota.").build();
        }
        if (ruleInput.getCenterType() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Il CenterType non può essere nullo.").build();
        }

        // Utilizza saveIfNotExists per evitare duplicati
        ruleRepository.saveIfNotExists(ruleInput.getKeyword(), ruleInput.getCenterType());
        
        // Recupera la regola appena salvata o quella esistente per restituirla
        ClassificationRule newRule = ruleRepository.findByKeyword(ruleInput.getKeyword())
                                                  .orElseThrow(() -> new WebApplicationException("Errore interno: regola non trovata dopo il salvataggio.", Response.Status.INTERNAL_SERVER_ERROR));

        log.info("Regola creata/esistente con successo: {}", newRule.getId());
        return Response.status(Response.Status.CREATED).entity(newRule).build();
    }

    //Aggiorna una regola di classificazione esistente tramite il suo ID.
    @PUT
    @Transactional
    @Path("/{id}/modify")
    public Response updateRule(@PathParam("id") Long id, RuleInput ruleInput) {
        log.info("Ricevuta richiesta di aggiornamento regola ID: {} con dati: {}", id, ruleInput);
        
        if (ruleInput.getKeyword() == null || ruleInput.getKeyword().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("La parola chiave non può essere vuota.").build();
        }
        if (ruleInput.getCenterType() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Il CenterType non può essere nullo.").build();
        }

        return ruleRepository.findByIdOptional(id)
                .map(existingRule -> {
                    existingRule.setKeyword(ruleInput.getKeyword());
                    existingRule.setCenterType(ruleInput.getCenterType());
                    log.info("Regola ID: {} aggiornata con successo.", id);
                    return Response.ok(existingRule).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    // Elimina una regola di classificazione tramite il suo ID.  
    @DELETE
    @Transactional
    @Path("/{id}/delete")
    public Response deleteRule(@PathParam("id") Long id) {
        log.info("Richiesta di cancellazione regola ID: {}", id);
        boolean deleted = ruleRepository.deleteById(id);
        if (deleted) {
            log.info("Regola ID: {} cancellata con successo.", id);
            return Response.noContent().build();
        } else {
            log.warn("Tentativo di cancellare regola ID: {} non trovata.", id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /*
     * Aggiunge una nuova regola di classificazione (o verifica se esiste già) e riclassifica
     * tutte le transazioni presenti nel database in base all'insieme di regole aggiornato.
     * Le transazioni classificate manualmente non verranno sovrascritte.
     */  
    @POST
    @Transactional
    @Path("/reclassify-all") // Nuovo path per riclassificare tutte le transazioni
    public Response reclassifyAllTransactions(RuleInput ruleInput) {
        log.info("Richiesta di riclassificazione di TUTTE le transazioni con nuova regola: {}", ruleInput);

        if (ruleInput.getKeyword() == null || ruleInput.getKeyword().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("La parola chiave non può essere vuota.").build();
        }
        if (ruleInput.getCenterType() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Il CenterType non può essere nullo.").build();
        }
        if (ruleInput.getCenterType() == CenterType.UNDEFINED) {
             return Response.status(Response.Status.BAD_REQUEST).entity("Non è possibile classificare manualmente una regola come UNDEFINED.").build();
        }

        try {
            // 1. Salva la nuova regola (o verifica se esiste già)
            ruleRepository.saveIfNotExists(ruleInput.getKeyword(), ruleInput.getCenterType());
            log.info("Regola '{}' con CenterType '{}' aggiunta o già esistente.", ruleInput.getKeyword(), ruleInput.getCenterType());

            // 2. Recupera TUTTE le transazioni dal database
            List<Transaction> allTransactions = transactionRepository.listAll();
            log.info("Trovate {} transazioni totali da riclassificare.", allTransactions.size());

            int reclassifiedCount = 0;
            // 3. Riclassifica ogni transazione
            for (Transaction transaction : allTransactions) {
                // Il metodo classifyTransaction ri-valuta tutte le regole e aggiorna la transazione
                // Solo se la transazione non è stata classificata manualmente
                if (Boolean.FALSE.equals(transaction.getIsManuallyClassified())) {
                    CenterType oldCenterType = transaction.getCenterType();
                    ruleEngineService.classifyTransaction(transaction);
                    
                    // Se il centerType è cambiato (cioè è stato riclassificato con successo)
                    if (transaction.getCenterType() != oldCenterType) {
                        transactionRepository.persist(transaction); // Persisti l'aggiornamento
                        reclassifiedCount++;
                        log.debug("Transazione ID {} riclassificata da {} a {}.", transaction.getTransactionId(), oldCenterType, transaction.getCenterType());
                    }
                } else {
                    log.debug("Transazione ID {} è classificata manualmente, saltata la riclassificazione automatica.", transaction.getTransactionId());
                }
            }

            log.info("Riclassificazione completata. {} transazioni sono state riclassificate/aggiornate.", reclassifiedCount);
            return Response.ok("Riclassificazione completata. " + reclassifiedCount + " transazioni sono state riclassificate/aggiornate.").build();

        } catch (Exception e) {
            log.error("Errore durante la riclassificazione di tutte le transazioni: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante la riclassificazione delle transazioni: " + e.getMessage()).build();
        }
    }
}
