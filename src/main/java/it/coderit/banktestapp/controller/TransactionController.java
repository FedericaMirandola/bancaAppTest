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
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/transactions")
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

    // --- ENDPOINT GET UNIFICATO PER INTERAGIRE CON I DATI SU DATABASE ---
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response transactions(
            @QueryParam("from") String dateFrom,              // Opzionale
            @QueryParam("to") String dateTo,                  // Opzionale
            @QueryParam("centerType") String centerTypeStr) { // Opzionale

        // 1. Determinazione dell'accountId effettivo
        final String effectiveAccountId = defaultAccountId;
        if (effectiveAccountId == null || effectiveAccountId.isEmpty()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("configurazione predefinita mancante").build();
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
                        .entity("Tipo center non valido. Valori possibili: COSTO, PROFITTO, UNDEFINED").build();
            }
        }

        // 4. Logica di Recupero e Filtro dei Movimenti
        try {
            // Chiamata al service per cercare le transazioni
            List<Transaction> transactions = transactionService.searchTransactions(effectiveAccountId, fromDateObj, toDateObj, centerType);

            if (transactions.isEmpty()) {
                log.info("nessuna transazione trovata per account " + effectiveAccountId + " con i criteri specificati.");   
            } else {
                log.info("Trovate " + transactions.size() + " transazioni per account " + effectiveAccountId + " con i criteri specificati.");
            }

            return Response.ok(transactions).build();

        } catch (Exception e) {
            log.error("Errore durante il recupero/filtro dei movimenti per account " + effectiveAccountId + ": " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante il recupero/filtro dei movimenti: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{transactionId}/manual-classify")
    @Transactional
    public Response manuallyClassify(
        @PathParam("transactionId") String transactionId,
        @QueryParam("centerType") String centerTypeStr) {

            if (centerTypeStr == null || centerTypeStr.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                .entity("Parametro centerType obbligatorio.").build();
            }

            CenterType newCenterType;
            try {
                newCenterType = CenterType.valueOf(centerTypeStr.toUpperCase());

                if(newCenterType == CenterType.UNDEFINED) {
                    return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Non si può impostare manualmente il cento UNDEFINED").build();
                }
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                .entity("Tipo centro non valido. Tipi accettati: COSTO o PROFITTO").build();
            }

            try{
                Transaction transactionToUpdate = transactionRepository.find("transactionId", transactionId).firstResult();
                
                if (transactionToUpdate == null) {
                    return Response.status(Response.Status.NOT_FOUND)
                    .entity("Trasazione ID: " + transactionId + " non trovata.").build();
                }

                transactionToUpdate.setCenterType(newCenterType);
                transactionToUpdate.setIsManuallyClassified(true);

                transactionRepository.persist(transactionToUpdate);

                log.info("Transazione ID " + transactionId + " classificata manualmente come " + newCenterType + ".");
                return Response.ok("Transazione ID: " + transactionId + " classificata manualmente come: " + newCenterType).build();

            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Errore durante la classificazione manuale" + e.getMessage()).build();
            }
        }
   
}