package it.coderit.banktestapp.controller;

import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.dto.ScaricaResult;
import it.coderit.banktestapp.model.Movimento;
import it.coderit.banktestapp.model.TipoCentro;
import it.coderit.banktestapp.repository.MovimentoRepository;
import it.coderit.banktestapp.service.MovimentoService;
import it.coderit.banktestapp.service.RuleEngineService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;


import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/movimenti")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MovimentoController {

    @Inject
    RuleEngineService ruleEngineService;

    @Inject
    MovimentoRepository movimentoRepository;

    @Inject
    MovimentoService movimentoService;

    @Inject
    Logger log;

    @ConfigProperty(name = "credem.account-id") 
    String accountId; 


    @GET
    @Path("/{accountId}/transactions") 
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTransactions(
            @PathParam("accountId") String accountId, // Recupera accountId dal path
            @QueryParam("from") String dateFrom,
            @QueryParam("to") String dateTo) {

        if (dateFrom == null || dateTo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parametri 'from' e 'to' obbligatori per recuperare le transazioni.").build();
        }

        try {
            
            List<Movimento> movimenti = movimentoService.leggiMovimentiPerAccountIdEData(accountId, dateFrom, dateTo);

            if (movimenti.isEmpty()) {
                // Potresti voler scaricare se non ci sono movimenti nel DB, o lasciare che lo scheduler lo faccia.
                // Per ora, leggiamo solo ciò che è già nel DB, per separare i ruoli.
                 return Response.status(Response.Status.NOT_FOUND)
                         .entity("Nessuna transazione trovata per il conto " + accountId + " nel periodo specificato. Prova a scaricare prima.")
                         .build();
            }

            return Response.ok(movimenti).build();

        } catch (Exception e) {
            log.error("Errore durante il recupero delle transazioni per account " + accountId + ": " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante il recupero delle transazioni: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/scarica")
    public Response scarica(@QueryParam("from") String from,
            @QueryParam("to") String to) {
        if (from == null || to == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parametri 'from' e 'to' obbligatori. Formato: YYYY-MM-dd")
                    .build();
        }

        movimentoService.scaricaEMemorizzaMovimenti(accountId, from, to);

        String msg = "Movimenti scaricati e salvati da " + from + " a " + to;
        return Response.ok(new ScaricaResult(msg)).build();
    }

    @POST
    @Path("/classify-and-save")
    @Transactional
    public Response classificaESalva(CredemTransactionResponse response) {
        // spostata la logica di persistenza e classificazione in MovimentoService.salvaMovimentiDaLista.
        // Questo endpoint dovrebbe ora delegare a quel metodo per mantenere la coerenza e riusabilità.
        if (response != null && response.booked != null) {
            movimentoService.salvaMovimentiDaLista(response.booked);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Il corpo della richiesta è vuoto o non contiene transazioni 'booked'.").build();
        }
    }

    @GET
    @Path("/by-centro")
    public Response movimentiPerTipoCentro(@QueryParam("tipo") String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parametro 'tipo' obbligatorio")
                    .build();
        }

        TipoCentro tipoCentro;
        try {
            tipoCentro = TipoCentro.fromName(tipo);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Tipo centro non valido, valori possibili: COSTO, PROFITTO")
                    .build();
        }

        List<Movimento> movimenti = movimentoService.leggiMovimentiPerTipoCentro(tipoCentro);
        return Response.ok(movimenti).build();
    }

}