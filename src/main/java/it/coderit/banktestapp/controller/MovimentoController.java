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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
    String defaultAccountId;

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

            LocalDate fromDateObj;
            LocalDate toDateObj;
            try {
                fromDateObj = LocalDate.parse(dateFrom);
                toDateObj = LocalDate.parse(dateTo);
            } catch (DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Formato data non valido. Usa il formato YYYY-MM-dd.")
                        .build();
            }
            if (fromDateObj.isAfter(toDateObj)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("La data 'from' non può essere dopo la data 'to'.")
                        .build();
            }

            List<Movimento> movimenti = movimentoService.leggiMovimentiPerAccountIdEData(accountId, dateFrom, dateTo);

            if (movimenti.isEmpty()) {
                // Potresti voler scaricare se non ci sono movimenti nel DB, o lasciare che lo
                // scheduler lo faccia.
                // Per ora, leggiamo solo ciò che è già nel DB, per separare i ruoli.
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Nessuna transazione trovata per il conto " + accountId
                                + " nel periodo specificato. Prova a scaricare prima.")
                        .build();
            }

            return Response.ok(movimenti).build();

        } catch (Exception e) {
            log.error("Errore durante il recupero delle transazioni per account " + accountId + ": " + e.getMessage(),
                    e);
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
        try {
            LocalDate fromDateObj = LocalDate.parse(from);
            LocalDate toDateObj = LocalDate.parse(to);

            if (fromDateObj.isAfter(toDateObj)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("La data 'from' non può essere successiva alla data 'to'.").build();
            }

            movimentoService.scaricaEMemorizzaMovimenti(defaultAccountId, from, to);

            String msg = "Movimenti scaricati e salvati da " + from + " a " + to;
            return Response.ok(new ScaricaResult(msg)).build();
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Formato data non valido per 'from' o 'to'. Utilizzare il formato YYYY-MM-DD.").build();
        } catch (Exception e) {
            log.error("Errore durante lo scaricamento dei movimenti da " + from + " a " + to + ": " + e.getMessage(),
                    e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante lo scaricamento dei movimenti: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/classify-and-save")
    @Transactional
    public Response classificaESalva(CredemTransactionResponse response) {
        // spostata la logica di persistenza e classificazione in
        // MovimentoService.salvaMovimentiDaLista.
        // Questo endpoint dovrebbe ora delegare a quel metodo per mantenere la coerenza
        // e riusabilità.
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
 //----------------------------------------------------------------------------------------------------
 //----------------------------------------------------------------------------------------------------
    @GET
    @Path("/filtra") // Nuovo endpoint unificato
    @Produces(MediaType.APPLICATION_JSON)
    public Response filtraMovimenti(
            @QueryParam("accountId") String accountId, // Ora come QueryParam
            @QueryParam("from") String dateFrom,
            @QueryParam("to") String dateTo,
            @QueryParam("tipoCentro") String tipoCentroStr, // Nuovo parametro per TipoCentro
            @QueryParam("scarica") @DefaultValue("false") boolean scarica) { // Parametro per abilitare lo scaricamento

        // 1. Validazione di base per accountId (può essere null se non fornito)
        final String effectiveAccountId = (accountId != null && !accountId.isEmpty()) ? accountId : defaultAccountId;
        if (effectiveAccountId == null || effectiveAccountId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parametro 'accountId' obbligatorio o configurazione predefinita mancante.").build();
        }

        LocalDate fromDateObj = null;
        LocalDate toDateObj = null;

        // 2. Parsing e validazione delle date, solo se fornite
        if (dateFrom != null && !dateFrom.isEmpty() && dateTo != null && !dateTo.isEmpty()) {
            try {
                fromDateObj = LocalDate.parse(dateFrom);
                toDateObj = LocalDate.parse(dateTo);
            } catch (DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Formato data non valido per 'from' o 'to'. Utilizzare il formato YYYY-MM-DD.").build();
            }

            if (fromDateObj.isAfter(toDateObj)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("La data 'from' non può essere successiva alla data 'to'.").build();
            }
        } else if ((dateFrom != null && !dateFrom.isEmpty()) || (dateTo != null && !dateTo.isEmpty())) {
            // Se solo uno dei due parametri data è fornito
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Per il filtro per data, entrambi i parametri 'from' e 'to' sono obbligatori.").build();
        }

        // 3. Parsing e validazione del TipoCentro, solo se fornito
        TipoCentro tipoCentro = null;
        if (tipoCentroStr != null && !tipoCentroStr.isEmpty()) {
            try {
                tipoCentro = TipoCentro.fromName(tipoCentroStr);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Tipo centro non valido. Valori possibili: COSTO, PROFITTO").build();
            }
        }

        // 4. Logica di Scaricamento (se richiesto e con date valide)
        if (scarica) {
            if (fromDateObj == null || toDateObj == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Per scaricare i movimenti, i parametri 'from' e 'to' sono obbligatori.").build();
            }
            try {
                log.info("Avvio scaricamento movimenti per accountId: " + effectiveAccountId + " da " + dateFrom + " a " + dateTo);
                movimentoService.scaricaEMemorizzaMovimenti(effectiveAccountId, dateFrom, dateTo);
                // Puoi decidere se vuoi restituire solo un messaggio di scaricamento o anche i movimenti appena scaricati
                // Per semplicità, qui restituisco solo il messaggio di scaricamento.
                // Se vuoi anche i movimenti, dovresti chiamare leggiMovimentiPerAccountIdEData dopo scaricaEMemorizzaMovimenti
                return Response.ok(new ScaricaResult("Movimenti scaricati e salvati da " + dateFrom + " a " + dateTo)).build();
            } catch (Exception e) {
                log.error("Errore durante lo scaricamento dei movimenti per account " + effectiveAccountId + ": " + e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Errore durante lo scaricamento dei movimenti: " + e.getMessage()).build();
            }
        }


        // 5. Logica di Recupero e Filtro dei Movimenti
        try {
            List<Movimento> movimenti;

            // Logica per chiamare il service in base ai parametri forniti
            if (fromDateObj != null && toDateObj != null) {
                // Filtra per accountId e range di date
                movimenti = movimentoService.leggiMovimentiPerAccountIdEData(effectiveAccountId, dateFrom, dateTo);
            } else if (tipoCentro != null) {
                // Filtra solo per TipoCentro (senza date, prende tutti i movimenti di quel tipo)
                movimenti = movimentoService.leggiMovimentiPerTipoCentro(tipoCentro);
            } else {
                // Nessun filtro specifico (solo accountId), recupera tutti i movimenti per quell'account (se il service lo supporta)
                // Se il tuo service non ha un metodo per "tutti i movimenti per account",
                // dovrai implementarlo o decidere di rendere obbligatorio almeno un filtro.
                // Per ora, assumiamo che leggiMovimentiPerAccountIdEData senza date restituisca tutto (dovrai adattarlo)
                // O potresti avere un metodo getAllMovimentiByAccountId
                // Per semplicità, qui userò un placeholder o assumerò che il service possa gestire null per le date.
                // ALTERNATIVA: Chiedere al service un metodo getAllMovimentiByAccountId(String accountId)
                // Se non esiste, dovresti decidere se permettere questa chiamata senza filtri o renderli obbligatori.
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Almeno un criterio di filtro (date o tipoCentro) è richiesto per recuperare i movimenti senza scaricare.").build();
                // O se vuoi permetterlo:
                // movimenti = movimentoService.leggiTuttiMovimentiPerAccountId(effectiveAccountId);
            }

            if (movimenti.isEmpty()) {
                // Messaggio più generico, dato che ora i filtri sono variabili
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Nessuna transazione trovata per i criteri specificati.").build();
            }

            return Response.ok(movimenti).build();

        } catch (Exception e) {
            log.error("Errore durante il recupero/filtro dei movimenti per account " + effectiveAccountId + ": " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante il recupero/filtro dei movimenti: " + e.getMessage()).build();
        }
    }


    // L'endpoint classificaESalva rimane separato in quanto è una POST
    @POST
    @Path("/classify-and-save")
    @Transactional
    public Response classifyAndSave(CredemTransactionResponse response) {
        if (response != null && response.booked != null) {
            try {
                movimentoService.salvaMovimentiDaLista(response.booked);
                return Response.ok().build();
            } catch (Exception e) {
                log.error("Errore durante la classificazione e il salvataggio dei movimenti: " + e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Errore durante la classificazione e il salvataggio: " + e.getMessage()).build();
            }
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Il corpo della richiesta è vuoto o non contiene transazioni 'booked'.").build();
        }
    }

}