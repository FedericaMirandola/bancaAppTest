package it.coderit.banktestapp.controller;

import java.util.List;

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
    jakarta.persistence.EntityManager em;

    @GET
    public List<ClassificationRule> listaRegole() {
        return regolaRepository.listAll();
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

        for (RuleInput in : input) {
            if (in.keyword == null || in.center == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("keyword e center sono campi obbligatori").build();
            }

            ClassificationRule nuova = new ClassificationRule();
            nuova.setKeyword(in.keyword.toLowerCase());
            nuova.setCenter(in.center);
            nuova.setJsonRule(in.jsonRule);

            regolaRepository.persist(nuova);
        }

        return Response.ok("Nuove rules create con successo!").build();
    }

    // endpoint per ottenere e modificare regola per ID della regola
    // Nel ClassificationRuleController.java
    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, RuleInput input) {
        ClassificationRule regola = regolaRepository.findById(id);
        if (regola == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Rule non trovata").build(); // Restituisci 404 se non trovata
        }

        // Aggiorna i campi (assicurati che input.keyword e input.center non siano
        // nulli qui,
        // o gestisci il caso in cui lo siano per permettere aggiornamenti parziali)
        if (input.keyword != null) {
            regola.setKeyword(input.keyword.toLowerCase());
        }
        if (input.center != null) {
            regola.setCenter(input.center);
        }
        // Questo potrebbe essere null, quindi imposta direttamente
        regola.setJsonRule(input.jsonRule);

        // Non serve regolaRepository.persist(regola); se l'oggetto è gestito da
        // EntityManager e @Transactional

        return Response.ok(regola).build(); // Restituisci la regola aggiornata
    }

    // endpoint per eliminare regola per ID della regola
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = regolaRepository.deleteById(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
            .entity("Rule non trovata")
            .build();
        }
        return Response.noContent().build();
    }
}
