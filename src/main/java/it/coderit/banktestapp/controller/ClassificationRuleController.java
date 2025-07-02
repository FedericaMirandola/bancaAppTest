package it.coderit.banktestapp.controller;

import java.util.List;
import java.util.Collections; // Import aggiunto per Collections.emptyList()
import org.jboss.logging.Logger; // Import aggiunto per Logger

import it.coderit.banktestapp.dto.RuleInput;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.repository.ClassificationRuleRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClassificationRuleController {

    @Inject
    ClassificationRuleRepository regolaRepository;

    @Inject
    // jakarta.persistence.EntityManager em; // Non è necessario iniettare EntityManager direttamente se usi Panache
    Logger log; // Iniettiamo il logger per una migliore gestione degli errori

    @GET
    public Response listaRegole() { // Cambiato il ritorno a Response per coerenza con le best practice REST
        try {
            List<ClassificationRule> rules = regolaRepository.listAll();
            // Restituisce 200 OK con una lista vuota se non ci sono regole, non 404
            return Response.ok(rules).build();
        } catch (Exception e) {
            log.error("Errore durante il recupero delle regole: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante il recupero delle regole: " + e.getMessage()).build();
        }
    }

    // endpoint per creare rules per parola chiave
    @POST
    @Path("/add")
    @Transactional
    public Response add(List<RuleInput> input) {
        if (input == null || input.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("I campi non possono essere vuoti").build();
        }

        try {
            for (RuleInput in : input) {
                if (in.keyword == null || in.keyword.isEmpty() || in.center == null) { // Aggiunto controllo .isEmpty()
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("keyword e center sono campi obbligatori").build();
                }

                ClassificationRule nuova = new ClassificationRule();
                nuova.setKeyword(in.keyword.toLowerCase());
                nuova.setCenter(in.center);
                nuova.setJsonRule(in.jsonRule); // Può essere null, va bene

                regolaRepository.persist(nuova);
            }
            return Response.ok("Nuove rules create con successo!").build();
        } catch (Exception e) {
            log.error("Errore durante la creazione delle rules: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante la creazione delle rules: " + e.getMessage()).build();
        }
    }

    // endpoint per ottenere e modificare regola per ID della regola
    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, RuleInput input) {
        if (input == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Il corpo della richiesta non può essere vuoto.").build();
        }
        try {
            ClassificationRule regola = regolaRepository.findById(id);
            if (regola == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Rule non trovata").build();
            }

            // Aggiorna solo se il campo non è null o vuoto nell'input
            if (input.keyword != null && !input.keyword.isEmpty()) { // Aggiunto controllo .isEmpty()
                regola.setKeyword(input.keyword.toLowerCase());
            }
            if (input.center != null) {
                regola.setCenter(input.center);
            }
            // LOGICA CORRETTA PER JSONRULE:
            // Se input.jsonRule è null, NON lo aggiorniamo, mantenendo il valore esistente.
            // Se input.jsonRule è una stringa vuota, allora lo impostiamo a null (o stringa vuota, a seconda della logica desiderata).
            // Per il test, se è null, lo ignoriamo. Se è una stringa vuota, lo impostiamo a null.
            if (input.jsonRule != null) {
                regola.setJsonRule(input.jsonRule.isEmpty() ? null : input.jsonRule);
            }
            // Non serve regolaRepository.persist(regola); se l'oggetto è gestito da
            // EntityManager e @Transactional (Panache lo fa automaticamente)

            return Response.ok(regola).build(); // Restituisci la regola aggiornata
        } catch (Exception e) {
            log.error("Errore durante l'aggiornamento della regola con ID " + id + ": " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante l'aggiornamento della regola: " + e.getMessage()).build();
        }
    }

    // endpoint per eliminare regola per ID della regola
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        try { // Aggiunto try-catch per gestire errori DB
            boolean deleted = regolaRepository.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Rule non trovata")
                        .build();
            }
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Errore durante l'eliminazione della regola con ID " + id + ": " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante l'eliminazione della regola: " + e.getMessage()).build();
        }
    }

    
    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        try {
            ClassificationRule rule = regolaRepository.findById(id);
            if (rule == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Rule non trovata").build();
            }
            return Response.ok(rule).build();
        } catch (Exception e) {
            log.error("Errore durante il recupero della regola con ID " + id + ": " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante il recupero della regola: " + e.getMessage()).build();
        }
    }
}
