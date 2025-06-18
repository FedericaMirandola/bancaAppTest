package it.coderit.banktestapp.controller;

import java.util.List;

import it.coderit.banktestapp.dto.RegolaInput;
import it.coderit.banktestapp.model.RegolaClassificazione;

import it.coderit.banktestapp.repository.RegolaClassificazioneRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegolaClassificazioneController {

    @Inject
    RegolaClassificazioneRepository regolaRepository;

    @Inject
    jakarta.persistence.EntityManager em;

    @GET
    public List<RegolaClassificazione> listaRegole() {
        return regolaRepository.listAll();
    }

   
    // endpoint per creare regole per parola chiave
    @POST
    @Path("/add")
    @Transactional
    public Response add(List<RegolaInput> input) {
        if (input == null || input.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("I campi non possono essere vuoti").build();

        }

        for (RegolaInput in : input) {
            if (in.parolaChiave == null || in.centro == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("parolaChiave e centro sono campi obbligatori").build();
            }

            RegolaClassificazione nuova = new RegolaClassificazione();
            nuova.setParolaChiave(in.parolaChiave.toLowerCase());
            nuova.setCentro(in.centro);
            nuova.setJsonRule(in.jsonRule);

            regolaRepository.persist(nuova);
        }

        return Response.ok("Nuove regole creata con successo!").build();
    }

    // endpoint per ottenere e modificare regola per ID della regola
    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, RegolaInput input) {
        RegolaClassificazione regola = regolaRepository.findById(id);
        return Response.ok(regola).build();
    }

    // endpoint per eliminare regola per ID della regola
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = regolaRepository.deleteById(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
