package it.coderit.banktestapp.controller;

import it.coderit.banktestapp.dto.RuleInput;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.repository.ClassificationRuleRepository;
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

    // restituisce tutte le regole
    @GET
    public List<ClassificationRule> getAllRules() {
        log.info("Richiesta di tutte le regole di classificazione.");
        return ruleRepository.listAll();
    }

    // restituisce una regola per id, passato come Pathparam
    @GET
    @Path("/{id}")
    public Response getRuleById(@PathParam("id") Long id) {
        log.info("Richiesta regola di classificazione con ID: {}", id);
        return ruleRepository.findByIdOptional(id)
                .map(rule -> Response.ok(rule).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    // endpoint per creare una nuova regola
    @POST
    @Transactional
    @Path("/add")
    public Response createRule(RuleInput ruleInput) {
        log.info("Ricevuta richiesta di creazione regola: {}", ruleInput);

        ClassificationRule newRule = new ClassificationRule(ruleInput.getKeyword(), ruleInput.getCenterType());
        ruleRepository.persist(newRule);
        log.info("Regola creata con successo: {}", newRule.getId());
        return Response.status(Response.Status.CREATED).entity(newRule).build();
    }

    // endpoint per ottenere e modificare regola per ID della regola
    @PUT
    @Transactional
    @Path("/{id}/modify")
    public Response updateRule(@PathParam("id") Long id, RuleInput ruleInput) {
        log.info("Ricevuta richiesta di aggiornamento regola ID: {} con dati: {}", id, ruleInput);
        return ruleRepository.findByIdOptional(id)
                .map(existingRule -> {
                    existingRule.setKeyword(ruleInput.getKeyword());
                    existingRule.setCenterType(ruleInput.getCenterType());
                    
                    log.info("Regola ID: {} aggiornata con successo.", id);
                    return Response.ok(existingRule).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    // endpoint per eliminare regola per ID della regola
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

}
