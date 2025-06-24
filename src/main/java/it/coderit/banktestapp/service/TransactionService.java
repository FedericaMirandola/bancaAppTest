package it.coderit.banktestapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.coderit.banktestapp.dto.CredemAccountResponse;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.dto.CredemTransactionResponse.TransactionData;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.CenterType;

import it.coderit.banktestapp.repository.TransactionRepository;
import it.coderit.banktestapp.repository.ClassificationRuleRepository;
import it.coderit.banktestapp.rest.CredemClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

// Import necessari per le query Panache dinamiche
import io.quarkus.panache.common.Parameters;

/**
 * Servizio per la gestione dei movimenti bancari, inclusi scaricamento,
 * salvataggio, classificazione e recupero filtrato.
 */
@ApplicationScoped
public class TransactionService {

    // --- Iniezione delle Dipendenze ---

    @Inject
    @RestClient // Inietta automaticamente il client REST Credem (reale o mockato)
    CredemClient credemClient;

    @Inject
    ClassificationRuleRepository regolaRepo; // Repository per le rules di classificazione

    @Inject
    TransactionRepository transactionRepo; // Repository per i movimenti

    @Inject
    RuleEngineService ruleEngineService; // Servizio per applicare le rules di classificazione

    @Inject
    ObjectMapper objectMapper; // Per la deserializzazione JSON (usato in loadFromFile)

    @Inject
    MockCbiAuthService mockCbiAuthService; // Servizio per ottenere token di autenticazione mockati

    // Logger per registrare eventi e errori
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    // Formattatore per le date ISO_OFFSET_DATE_TIME (es. "2024-06-23T10:00:00+02:00")
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // Proprietà di configurazione iniettata dal file application.properties
    @ConfigProperty(name = "credem.psu-id")
    String psuId; // ID dell'utente dei servizi di pagamento


    @ConfigProperty(name = "scheduler.load-from-file", defaultValue = "false")
    boolean loadFromFile;

    @ConfigProperty(name = "scheduler.test-data.filenames")
    List<String> testDataFilenames;

    // ProprietÃ  per l'account ID predefinito/preferito/fallback
    @ConfigProperty(name = "credem.default-account-id")
    String defaultAccountIdForOperations;

    // --- Metodi di Business Logic ---

    /**
     * Scarica e memorizza i movimenti bancari per un dato account ID in un range di date.
     * Utilizza la paginazione per scaricare tutti i movimenti disponibili.
     *
     * @param accountId L'ID del conto da cui scaricare i movimenti.
     * @param from      La data di inizio del periodo (formato "YYYY-MM-DD").
     * @param to        La data di fine del periodo (formato "YYYY-MM-DD").
     */
    @Transactional // Garantisce che l'intera operazione sia atomica (o tutto, o niente)
    public void downloadAndSave(String accountId, String from, String to) {
        log.info("Inizio scaricamento e memorizzazione movimenti per accountId: {} da {} a {}.", accountId, from, to);

        int offset = 0; // Offset per la paginazione dell'API Credem
        int limit = 100; // Limite di transazioni per singola richiesta API
        boolean hasMore = true; // Flag per controllare se ci sono altre pagine da scaricare

        String token = mockCbiAuthService.getAccessToken(); // Ottengo il token Bearer mockato (o reale)

        // Ciclo per gestire la paginazione delle risposte dall'API Credem
        while (hasMore) {
            log.info("Scaricamento pagina con offset: {}", offset);

            // Effettua la chiamata al client REST per ottenere le transazioni
            CredemTransactionResponse response = credemClient.getTransactions(
                    accountId, // Account ID richiesto dall'API
                    from,      // Data di inizio
                    to,        // Data di fine
                    limit,     // Limite per pagina
                    offset,    // Offset corrente
                    psuId,     // ID utente del servizio di pagamento
                    token);    // Token di autenticazione

            // Controlla se la risposta contiene movimenti e li salva
            if (response != null && response.booked != null && !response.booked.isEmpty()) {
                // Salva i movimenti ottenuti, passando l'accountId per assicurare la consistenza
                saveTransactionsFromDTOList(response.booked, accountId); // Passiamo accountId esplicitamente
                offset += limit; // Incrementa l'offset per la prossima pagina
                hasMore = response.booked.size() >= limit; // Continua se ci sono potenzialmente altre pagine
            } else {
                hasMore = false; // Nessun transaction o pagina vuota, termina il ciclo
            }
        }
        log.info("Scaricamento movimenti completato da {} a {}.", from, to);
    }

    /**
     * Salva una lista di DTO di transazioni nel database come Transaction.
     * Applica le rules di classificazione ed evita duplicati basandosi sull'ID transazione.
     *
     * @param dtoList           La lista di DTO CredemTransactionResponse.TransactionData.
     * @param defaultAccountId  L'ID del conto da usare se il DTO non lo specifica.
     */
    @Transactional
    public void saveTransactionsFromDTOList(List<TransactionData> dtoList, String defaultAccountId) {
        log.info("Inizio salvataggio movimenti da lista. Transazioni da processare: {}", (dtoList != null ? dtoList.size() : 0));
        if (dtoList == null || dtoList.isEmpty()) {
            return; // Nessun transaction da salvare
        }

        for (TransactionData dto : dtoList) {
            Transaction transaction = fromDto(dto); // Converte DTO in entità Transaction

            // Se l'accountId non è presente nel DTO (caso limite), usa il default fornito
            if (transaction.getAccountId() == null || transaction.getAccountId().isBlank()) {
                transaction.setAccountId(defaultAccountId);
                log.warn("Transaction con transactionId {} non aveva accountId nel DTO, assegnato: {}", transaction.getTransactionId(), defaultAccountId);
            }
            
            ruleEngineService.classifyTransaction(transaction); // Applica le rules di classificazione

            // Evita duplicati: persistito solo se non esiste già un transaction con lo stesso transactionId
            if (transactionRepo.find("transactionId", transaction.getTransactionId()).firstResultOptional().isEmpty()) {
                transactionRepo.persist(transaction);
                log.debug("Persistito transaction con ID transazione: {}", transaction.getTransactionId());
            } else {
                log.debug("Transaction già presente con ID transazione: {}", transaction.getTransactionId());
            }
        }
        log.info("Salvataggio movimenti completato.");
    }

    /**
     * Converte un DTO `TransactionData` in un'entità `Transaction`.
     *
     * @param dto Il DTO da convertire.
     * @return L'entità `Transaction` risultante.
     */
    private Transaction fromDto(TransactionData dto) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(dto.transactionId);

        // Parsing delle date da String a OffsetDateTime
        if (dto.bookingDate != null) {
            transaction.setBookingDate(OffsetDateTime.parse(dto.bookingDate, ISO_FORMATTER));
        }
        if (dto.valueDate != null) {
            transaction.setValueDate(OffsetDateTime.parse(dto.valueDate, ISO_FORMATTER));
        }

        // Mappatura importo e valuta
        if (dto.transactionAmount != null) {
            transaction.setAmount(dto.transactionAmount.amount);
            transaction.setCurrency(dto.transactionAmount.currency.name()); // Converte Currency enum in String
        }

        // Mappatura altri campi
        transaction.setRemittanceInformation(dto.remittanceInformationUnstructured);
        transaction.setCreditorName(dto.creditorName);
        transaction.setDebtorName(dto.debtorName);
        transaction.setBankTransactionCode(dto.bankTransactionCode);
        transaction.setProprietaryBankTransactionCode(dto.proprietaryBankTransactionCode);
        transaction.setAdditionalInformation(dto.additionalInformation);

        // Imposta l'accountId dal DTO. Se nullo/vuoto, la logica di 'saveTransactionsFromDTOList' lo gestirà.
        transaction.setAccountId(dto.accountId); 

        return transaction;
    }

    /**
     * **NUOVO METODO UNIFICATO PER IL FILTRO**
     * Recupera una lista di movimenti dal database applicando filtri dinamici.
     * Supporta filtro per accountId, range di date e tipo di center di costo/profitto.
     * Tutti i parametri di filtro, tranne accountId, sono opzionali.
     *
     * @param accountId L'ID del conto (obbligatorio a livello di controller, ma passato qui).
     * @param fromDate  Data di inizio del range (opzionale).
     * @param toDate    Data di fine del range (opzionale).
     * @param centerType Tipo di center (COSTO/PROFITTO) (opzionale).
     * @return Una lista di Transaction che soddisfano i criteri di filtro.
     */
    public List<Transaction> searchTransactions(
            String accountId,
            LocalDate fromDate, // LocalDate dal controller
            LocalDate toDate,   // LocalDate dal controller
            CenterType centerType) {

        // Inizializza la query Panache con il filtro obbligatorio accountId
        StringBuilder queryBuilder = new StringBuilder("accountId = :accountId");
        Parameters parameters = Parameters.with("accountId", accountId);

        // Conversione di LocalDate a OffsetDateTime per la query al database
        // (il campo bookingDate in Transaction è OffsetDateTime)
        OffsetDateTime fromOffsetDateTime = null;
        OffsetDateTime toOffsetDateTime = null;

        if (fromDate != null) {
            fromOffsetDateTime = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC); // Inizio del giorno
        }
        if (toDate != null) {
            toDate = toDate.plusDays(1); // Incremento la data "TO" per includere l'intero giorno
            toOffsetDateTime = toDate.atStartOfDay().atOffset(ZoneOffset.UTC);   // Inizio del giorno successivo per un range esclusivo
        }

        // Aggiungi filtro per range di date se entrambi i parametri sono presenti
        if (fromOffsetDateTime != null && toOffsetDateTime != null) {
            queryBuilder.append(" and bookingDate >= :fromBookingDate AND bookingDate < :toBookingDate"); // Usiamo < per includere fino all'inizio del giorno successivo
            parameters.and("fromBookingDate", fromOffsetDateTime).and("toBookingDate", toOffsetDateTime);
        }

        // Aggiungi filtro per centerType se il parametro è presente
        if (centerType != null) {
            queryBuilder.append(" and centerType = :centerType");
            parameters.and("centerType", centerType);
        }

        // Esegue la query dinamica sul repository e restituisce la lista dei movimenti
        // Il TransactionRepository (PanacheRepository) ha un metodo find che accetta
        // una stringa di query e un oggetto Parameters.
        return transactionRepo.find(queryBuilder.toString(), parameters).list();
    }

    // --- Vecchi Metodi di Query (Commentati - possono essere rimossi) ---

    /*
     * Questo metodo è ora inglobato da searchTransactions.
     * Non più chiamato direttamente dal controller unificato.
     *
    @Transactional
    public List<Transaction> leggiMovimentiPerAccountIdEData(String accountId, String from, String to) {
        if (from == null || to == null) {
            log.warn("Parametri di data 'from' o 'to' nulli in leggiMovimentiPerAccountIdEData.");
            return List.of();
        }

        OffsetDateTime fromDate = LocalDate.parse(from).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDate = LocalDate.parse(to).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        // Delega al repository per filtrare anche per accountId
        return transactionRepo.findByAccountIdAndDateRange(accountId, fromDate, toDate);
    }
    */

    /*
     * Questo metodo è ora inglobato da searchTransactions.
     * Non più chiamato direttamente dal controller unificato.
     *
    public List<Transaction> leggiMovimentiPerCenterType(CenterType centerType) {
        return transactionRepo.findByCentroTipo(centerType);
    }
    */

    // --- Metodo di Importazione da File (lasciato invariato, non chiamato dal controller) ---

    /**
     * Importa movimenti da un file JSON locale. Utile per testing o inizializzazione.
     *
     * @param filename Il nome del file JSON (es. "transactions.json") nella cartella test-data.
     * @return Una lista (potenzialmente vuota) di movimenti importati.
     * @throws IOException Se il file non è trovato o il formato JSON è invalido.
     */
    @Transactional
    public List<Transaction> loadFromFile(String filename) throws IOException {
        log.info("Tentativo di importare movimenti dal file: {}", filename);
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-data/" + filename);
        if (inputStream == null) {
            throw new IOException("File non trovato: " + filename);
        }

        String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        CredemTransactionResponse response = objectMapper.readValue(jsonContent, CredemTransactionResponse.class);
        if (response == null || response.booked == null) {
            throw new IOException("Formato JSON non valido o nessuna transazione 'booked' in " + filename);
        }

        // Qui assumiamo che l'accountId sia già presente nel JSON o si debba usare un default.
        // Se il file è per un account specifico, potresti voler aggiungere un parametro qui.
        saveTransactionsFromDTOList(response.booked, "DEFAULT_ACCOUNT_FOR_IMPORT"); // Usa un default o aggiungi un parametro al metodo loadFromFile

        log.info("Importazione dal file {} completata. Movimenti salvati.", filename);
        return List.of(); // Questo metodo restituisce sempre una lista vuota dopo il salvataggio
    }

     public String getAccountIdForOperations() {
        log.info("Tentativo di recuperare un accountId dinamico da CredemClient.");
        String token = mockCbiAuthService.getAccessToken(); // Ottiene il token di autenticazione simulato

        try {
            // Effettua la chiamata al client REST per ottenere la lista degli account
            CredemAccountResponse accountResponse = credemClient.getAccounts(psuId, token);

            if (accountResponse != null && accountResponse.accounts != null && !accountResponse.accounts.isEmpty()) {
                // 1. Cerca l'accountId configurato come preferito
                Optional<String> preferredAccountId = accountResponse.accounts.stream()
                    .filter(account -> defaultAccountIdForOperations.equals(account.resourceId))
                    .map(account -> account.resourceId)
                    .findFirst();

                if (preferredAccountId.isPresent()) {
                    log.info("AccountId preferito '{}' recuperato con successo dalla lista degli account.", preferredAccountId.get());
                    return preferredAccountId.get();
                } else {
                    // 2. Se l'accountId preferito non Ã¨ nella lista, usa il primo disponibile
                    String firstAccountId = accountResponse.accounts.get(0).resourceId;
                    log.warn("AccountId preferito '{}' non trovato nella lista degli account. Utilizzo il primo account disponibile: {}", defaultAccountIdForOperations, firstAccountId);
                    return firstAccountId;
                }
            } else {
                // 3. Nessun account trovato, usa il default configurato come fallback
                log.warn("Nessun account trovato tramite CredemClient. Utilizzo l'accountId predefinito di fallback: {}", defaultAccountIdForOperations);
                return defaultAccountIdForOperations;
            }
        } catch (Exception e) {
            // 4. Errore nella chiamata, usa il default configurato come fallback
            log.error("Errore durante il recupero dinamico degli account da CredemClient. Utilizzo l'accountId predefinito di fallback: {}. Errore: {}", defaultAccountIdForOperations, e.getMessage());
            return defaultAccountIdForOperations;
        }
    }
}