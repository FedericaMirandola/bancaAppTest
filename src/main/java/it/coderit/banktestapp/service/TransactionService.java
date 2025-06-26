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
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.coderit.banktestapp.dto.CredemAccountResponse;
import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.dto.CredemTransactionResponse.TransactionData;
import it.coderit.banktestapp.dto.CredemSingleAccountResponse;
import it.coderit.banktestapp.dto.CredemBalancesResponse;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.CenterType;

import it.coderit.banktestapp.repository.TransactionRepository;
import it.coderit.banktestapp.repository.ClassificationRuleRepository;
import it.coderit.banktestapp.rest.CredemClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkus.panache.common.Parameters;

/**
 * Servizio per la gestione dei movimenti bancari, inclusi scaricamento,
 * salvataggio, classificazione e recupero filtrato e getione flow
 * autenticazione
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
    // Formattatore per le date ISO_OFFSET_DATE_TIME (es.
    // "2024-06-23T10:00:00+02:00")
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

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

    @ConfigProperty(name = "cbi.consent-mock-id")
    String mockConsentId;

    // --- Metodi di Business Logic ---

    /**
     * Scarica e memorizza i movimenti bancari per un dato account ID in un range di
     * date.
     * Utilizza la paginazione per scaricare tutti i movimenti disponibili.
     
     */
    @Transactional // Garantisce che l'intera operazione sia atomica (o tutto, o niente)
    public void downloadAndSave(String accountId, String from, String to) {
        log.info("Inizio scaricamento e memorizzazione movimenti per accountId: {} da {} a {}.", accountId, from, to);

        // LOGICA DA FILE
        if (loadFromFile) {
            log.info("Caricamento movimenti da file di test: {}", testDataFilenames);
            if (testDataFilenames == null || testDataFilenames.isEmpty()) {
                log.warn("Nessun file di test specificato per il caricamento dei movimenti.");
                return; // Esce se non ci sono file da caricare
            }
            for (String filename : testDataFilenames) {
                try {
                    loadFromFile(filename, accountId);
                } catch (IOException e) {
                    log.error("Errore durante il caricamento del file {}: {}", filename, e.getMessage());
                }
            }
            log.info("caricamento da file completato.");
            return; // Esce dopo il caricamento da file
        }

        // LOGICA DA API
        int offset = 0; // Offset per la paginazione dell'API Credem
        int limit = 100; // Limite di transazioni per singola richiesta API
        boolean hasMore = true; // Flag per controllare se ci sono altre pagine da scaricare

        String token = mockCbiAuthService.getAccessToken(); // Ottengo il token Bearer mockato (o reale)
        String xRequestId = "req-" + java.util.UUID.randomUUID().toString(); // ID di richiesta mockato (può essere
                                                                             // generato dinamicamente se necessario)
        String dateHeader = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER); // Data corrente in formato
                                                                                            // ISO (es. "2024-06-23")
        // Ciclo per gestire la paginazione delle risposte dall'API Credem
        while (hasMore) {
            log.info("Scaricamento pagina con offset: {}", offset);

            // Effettua la chiamata al client REST per ottenere le transazioni
            CredemTransactionResponse response = credemClient.getTransactions(
                    psuId,
                    token,
                    xRequestId,
                    mockConsentId,
                    dateHeader,
                    "mock-digest", // valore mock
                    "mock-signature", // valore mock
                    "mock-tpp-cert", // valore mock
                    "mock-psu-auth", // valore mock
                    "127.0.0.1", // Valore mock
                    "mock-aspsp-id",
                    accountId,
                    from,
                    to,
                    limit,
                    offset);

            // Controlla se la risposta contiene movimenti e li salva
            if (response != null && response.booked != null && !response.booked.isEmpty()) {
                // Salva i movimenti ottenuti, passando l'accountId per assicurare la
                // consistenza
                saveTransactionsFromDTOList(response.booked, accountId); // Passiamo accountId esplicitamente
                offset += limit; // Incrementa l'offset per la prossima pagina
                hasMore = response.booked.size() >= limit; // Continua se ci sono potenzialmente altre pagine
            } else {
                hasMore = false; // Nessun transaction o pagina vuota, termina il ciclo
            }
        }
        log.info("Scaricamento movimenti completato da {} a {}.", from, to);
    }

    @Transactional
    public void saveTransactionsFromDTOList(List<TransactionData> dtoList, String defaultAccountId) {
        log.info("Inizio salvataggio movimenti da lista. Transazioni da processare: {}",
                (dtoList != null ? dtoList.size() : 0));
        if (dtoList == null || dtoList.isEmpty()) {
            return; // Nessun transaction da salvare
        }

        for (TransactionData dto : dtoList) {
            Transaction transaction = fromDto(dto); // Converte DTO in entità Transaction

            // Se l'accountId non è presente nel DTO (caso limite), usa il default fornito
            if (transaction.getAccountId() == null || transaction.getAccountId().isBlank()) {
                transaction.setAccountId(defaultAccountId);
                log.warn("Transaction con transactionId {} non aveva accountId nel DTO, assegnato: {}",
                        transaction.getTransactionId(), defaultAccountId);
            }

            ruleEngineService.classifyTransaction(transaction); // Applica le rules di classificazione

            // Evita duplicati: persistito solo se non esiste già un transaction con lo
            // stesso transactionId
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

        // Imposta l'accountId dal DTO. Se nullo/vuoto, la logica di
        // 'saveTransactionsFromDTOList' lo gestirà.
        transaction.setAccountId(dto.accountId);

        return transaction;
    }

    public List<Transaction> searchTransactions(
            String accountId,
            LocalDate fromDate, // LocalDate dal controller
            LocalDate toDate, // LocalDate dal controller
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
            toOffsetDateTime = toDate.atStartOfDay().atOffset(ZoneOffset.UTC); // Inizio del giorno successivo per un
                                                                               // range esclusivo
        }

        // Aggiungi filtro per range di date se entrambi i parametri sono presenti
        if (fromOffsetDateTime != null && toOffsetDateTime != null) {
            queryBuilder.append(" and bookingDate >= :fromBookingDate AND bookingDate < :toBookingDate"); // Usiamo <
                                                                                                          // per
                                                                                                          // includere
                                                                                                          // fino
                                                                                                          // all'inizio
                                                                                                          // del giorno
                                                                                                          // successivo
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

    // --- Metodo di Importazione da File (lasciato invariato, non chiamato dal
    // controller) ---

    /**
     * Importa movimenti da un file JSON locale. Utile per testing o
     * inizializzazione.
     *
     * @param filename Il nome del file JSON (es. "transactions.json") nella
     *                 cartella test-data.
     * @return Una lista (potenzialmente vuota) di movimenti importati.
     * @throws IOException Se il file non è trovato o il formato JSON è invalido.
     */
    @Transactional
    public void loadFromFile(String filename, String targetAccountId) throws IOException {
        log.info("Tentativo di importare movimenti dal file: {}", filename);
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("test-data/" + filename);
        if (inputStream == null) {
            throw new IOException("File non trovato: test-data/" + filename);
        }

        String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        CredemTransactionResponse response = objectMapper.readValue(jsonContent, CredemTransactionResponse.class);
        if (response == null || response.booked == null) {
            throw new IOException("Formato JSON non valido o nessuna transazione 'booked' in " + filename);
        }

        // Passa targetAccountId al metodo che salva le transazioni
        saveTransactionsFromDTOList(response.booked, targetAccountId);

        log.info("Importazione dal file {} completata. Movimenti salvati.", filename);
        // Questo metodo ora è void, non restituisce List<Transaction>
    }

    public String getAccountIdForOperations() {
        log.info("Tentativo di recuperare un accountId dinamico da CredemClient.");
        String token = mockCbiAuthService.getAccessToken(); // Ottiene il token di autenticazione simulato

        String xRequestId = "req-" + UUID.randomUUID().toString(); // Genera un ID di richiesta unico
        String dateHeader = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER); // Formato RFC 1123 per
                                                                                            // l'header Date
        try {
            // Effettua la chiamata al client REST per ottenere la lista degli account
            CredemAccountResponse accountResponse = credemClient.getAccounts(psuId, token, mockConsentId, xRequestId,
                    dateHeader);

            if (accountResponse != null && accountResponse.accounts != null && !accountResponse.accounts.isEmpty()) {
                // 1. Cerca l'accountId configurato come preferito
                Optional<String> preferredAccountId = accountResponse.accounts.stream()
                        .filter(account -> defaultAccountIdForOperations.equals(account.resourceId))
                        .map(account -> account.resourceId)
                        .findFirst();

                if (preferredAccountId.isPresent()) {
                    log.info("AccountId preferito '{}' recuperato con successo dalla lista degli account.",
                            preferredAccountId.get());
                    return preferredAccountId.get();
                } else {
                    // 2. Se l'accountId preferito non Ã¨ nella lista, usa il primo disponibile
                    String firstAccountId = accountResponse.accounts.get(0).resourceId;
                    log.warn(
                            "AccountId preferito '{}' non trovato nella lista degli account. Utilizzo il primo account disponibile: {}",
                            defaultAccountIdForOperations, firstAccountId);
                    return firstAccountId;
                }
            } else {
                // 3. Nessun account trovato, usa il default configurato come fallback
                log.warn(
                        "Nessun account trovato tramite CredemClient. Utilizzo l'accountId predefinito di fallback: {}",
                        defaultAccountIdForOperations);
                return defaultAccountIdForOperations;
            }
        } catch (Exception e) {
            // 4. Errore nella chiamata, usa il default configurato come fallback
            log.error(
                    "Errore durante il recupero dinamico degli account da CredemClient. Utilizzo l'accountId predefinito di fallback: {}. Errore: {}",
                    defaultAccountIdForOperations, e.getMessage());
            return defaultAccountIdForOperations;
        }
    }

    public CredemSingleAccountResponse getSpecificAccountDetails(String accountId, Boolean withBalance) {
        log.info("Tentativo di recuperare i dettagli per l'account: {} con saldo: {}", accountId, withBalance);
        try {
            String token = mockCbiAuthService.getAccessToken();
            String xRequestId = "req-" + java.util.UUID.randomUUID().toString(); // Genera un ID di richiesta unico
            String dateHeader = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER); // Formato RFC 1123 per l'header Date

            // chiamata al REST Client per ottenere i dettagli di un singolo conto
            CredemSingleAccountResponse response = credemClient.getAccountDetails(
                    accountId,
                    mockConsentId,
                    psuId,
                    token,
                    xRequestId,
                    dateHeader,
                    withBalance);

            // Inizio del blocco 'if' esterno
            if (response != null && response.account != null) {
                log.info("Dettagli account recuperati per: {}. IBAN: {}", accountId, response.account.iban);
                
                // Inizio del blocco 'if' interno per i saldi
                if (response.account.balances != null && !response.account.balances.isEmpty()) { // Controllo anche se la lista non è vuota
                    response.account.balances.forEach(balance -> { // 'balance' qui è di tipo AccountBalance
                        log.info("Saldo disponibile: {} {} (Tipo: {})",
                                balance.amount,
                                balance.currency,
                                balance.balanceType); 
                    });
                } else { // Questo else si riferisce al blocco 'if' interno (se la lista dei saldi è null o vuota)
                    log.info("Nessun saldo disponibile per l'account: {}", accountId); // Messaggio più specifico
                }
            // Fine del blocco 'if' interno
            } else { // Questo else si riferisce al blocco 'if' esterno (se response o response.account sono null)
                log.warn("Nessun dettaglio account trovato per: {} o risposta malformata", accountId);
            }
            // Fine del blocco 'if' esterno

            return response;
        } catch (Exception e) {
            log.error("Errore durante il recupero dei dettagli dell'account {}: {}", accountId, e.getMessage());
            return null;
        }
    }


    public CredemBalancesResponse getBalancesForSpecificAccount(String accountId) {
        log.info("Tentativo di recuperare i saldi per l'account: {}", accountId);
        try {
            String token = mockCbiAuthService.getAccessToken();
            String xRequestId = "req-" + java.util.UUID.randomUUID().toString(); // Genera un ID di richiesta unico
            String dateHeader = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER); // Formato RFC 1123 per
                                                                                                // l'header Date

            // Chiamata al REST Client per ottenere i saldi di un singolo conto
            CredemBalancesResponse response = credemClient.getAccountBalances( 
                    accountId,
                    mockConsentId,
                    psuId,
                    token,
                    xRequestId,
                    dateHeader);

            if (response != null && response.balances != null && !response.balances.isEmpty()) {
                log.info("Saldi recuperati per l'account: {}", accountId);
                response.balances.forEach(balance -> {
                    log.info("Saldo: {} {}, Tipo: {}, Data Ultimo Aggiornamento: {}",
                            balance.amount, balance.currency, balance.balanceType, balance.lastChangeDateTime);
                });
            } else {
                log.warn("Nessun saldo trovato per l'account: {} o risposta malformata", accountId);
            }
            return response;
        } catch (Exception e) {
            log.error("Errore durante il recupero dei saldi dell'account {}: {}", accountId, e.getMessage());
            return null;
        }
    }
}