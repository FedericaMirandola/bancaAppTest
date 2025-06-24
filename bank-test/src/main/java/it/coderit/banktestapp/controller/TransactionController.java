package it.coderit.banktestapp.controller;

import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.dto.DownloadResult;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.CenterType;
import it.coderit.banktestapp.repository.TransactionRepository;
import it.coderit.banktestapp.service.TransactionService;
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
public class TransactionController {

    @Inject
    RuleEngineService ruleEngineService;

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    TransactionService transactionService;

    @Inject
    Logger log;

    @ConfigProperty(name = "credem.account-id")
    String defaultAccountId;
    /* 
    // --- Endpoint Originale per Transazioni (potrebbe essere deprecato/rimosso in futuro) ---
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
                        .entity("La data 'from' non può essere successiva alla data 'to'.")
                        .build();
            }

            
            List<Transaction> movimenti = transactionService.searchTransactions(accountId, fromDateObj, toDateObj, null); // Passa null per centerType
            if (movimenti.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Nessuna transazione trovata per il conto " + accountId
                                + " nel periodo specificato. Prova a scaricare prima.")
                        .build();
            }
            return Response.ok(movimenti).build();

        } catch (Exception e) {
            log.error("Errore durante il recupero delle transazioni per account " + accountId + ": " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante il recupero delle transazioni: " + e.getMessage()).build();
        }
    }

    // --- Endpoint Originale per Scaricare (potrebbe essere deprecato/rimosso in futuro, inglobato da /filtra) ---
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

            transactionService.downloadAndSave(defaultAccountId, from, to);

            String msg = "Movimenti scaricati e salvati da " + from + " a " + to;
            return Response.ok(new DownloadResult(msg)).build();
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Formato data non valido per 'from' o 'to'. Utilizzare il formato YYYY-MM-DD.").build();
        } catch (Exception e) {
            log.error("Errore durante lo scaricamento dei movimenti da " + from + " a " + to + ": " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante lo scaricamento dei movimenti: " + e.getMessage()).build();
        }
    } */
    /* probabilmente nn disponibile per cbi globe
    // --- Endpoint per ricevere tramite webhook i dati ---
    @POST
    @Path("/webhook") 
    @Transactional
    public Response classifyAndSave(CredemTransactionResponse response) {
        if (response != null && response.booked != null) {
            try {
                // Chiama il service per salvare e classificare.
                // decidere quale accountId passare qui, dato che l'endpoint POST non lo riceve.
                // si può usare defaultAccountId o renderlo un QueryParam/Path
                // Per coerenza con il resto si usa defaultAccountId come fallback.
                transactionService.saveTransactionsFromDTOList(response.booked, defaultAccountId);
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
    } */
    /* 
    // --- Endpoint Originale per By Centro (potrebbe essere deprecato/rimosso in futuro, inglobato da /filtra) ---
    @GET
    @Path("/by-center")
    public Response movimentiPerCenterType(@QueryParam("tipo") String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parametro 'tipo' obbligatorio")
                    .build();
        }

        CenterType centerType;
        try {
            centerType = CenterType.fromName(tipo);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Tipo center non valido, valori possibili: COSTO, PROFITTO")
                    .build();
        }
         
        
        List<Transaction> movimenti = transactionService.leggiMovimentiPerCenterType(centerType);
        return Response.ok(movimenti).build();
    }
    */
   
    // --- ENDPOINT GET UNIFICATO PER INTERAGIRE CON I DATI SU DATABASE ---
    @GET
    
    @Produces(MediaType.APPLICATION_JSON)
    public Response movimenti(
            @QueryParam("accountId") String accountId,         // Opzionale: se non fornito, usa defaultAccountId
            @QueryParam("from") String dateFrom,              // Opzionale: data di inizio del range
            @QueryParam("to") String dateTo,                  // Opzionale: data di fine del range
            @QueryParam("centerType") String centerTypeStr,   // Opzionale: CenterType (COSTO/PROFITTO)
            @QueryParam("scarica") @DefaultValue("false") boolean scarica) { // Opzionale: per forzare lo scaricamento

        // 1. Determinazione dell'accountId effettivo
        final String effectiveAccountId = (accountId != null && !accountId.isEmpty()) ? accountId : defaultAccountId;
        if (effectiveAccountId == null || effectiveAccountId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Parametro 'accountId' obbligatorio o configurazione predefinita mancante.").build();
        }

        LocalDate fromDateObj = null;
        LocalDate toDateObj = null;

        // 2. Parsing e validazione delle date, solo se *entrambe* fornite
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
            // Se solo uno dei due parametri data è fornito, è un errore di input
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Per il filtro per data, entrambi i parametri 'from' e 'to' sono obbligatori.").build();
        }

        // 3. Parsing e validazione del CenterType, solo se fornito
        CenterType centerType = null;
        if (centerTypeStr != null && !centerTypeStr.isEmpty()) {
            try {
                centerType = CenterType.fromName(centerTypeStr);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Tipo center non valido. Valori possibili: COSTO, PROFITTO").build();
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
                transactionService.downloadAndSave(effectiveAccountId, dateFrom, dateTo);
                return Response.ok(new DownloadResult("Movimenti scaricati e salvati da " + dateFrom + " a " + dateTo)).build();
            } catch (Exception e) {
                log.error("Errore durante lo scaricamento dei movimenti per account " + effectiveAccountId + ": " + e.getMessage(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Errore durante lo scaricamento dei movimenti: " + e.getMessage()).build();
            }
        }

        // 5. Logica di Recupero e Filtro dei Movimenti
        try {
            // Chiamata al nuovo metodo unificato del service
            List<Transaction> movimenti = transactionService.searchTransactions(effectiveAccountId, fromDateObj, toDateObj, centerType);

            if (movimenti.isEmpty()) {
                //cambiare con lista vuota
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
}