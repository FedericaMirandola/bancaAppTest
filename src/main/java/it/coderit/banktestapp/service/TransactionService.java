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

import java.util.Base64; 

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.coderit.banktestapp.CBISimulation.CredemAccountService;
import it.coderit.banktestapp.CBISimulation.MockCbiAuthService;
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


@ApplicationScoped
public class TransactionService {

    @Inject
    @RestClient
    CredemClient credemClient;

    @Inject
    ClassificationRuleRepository regolaRepo;

    @Inject
    TransactionRepository transactionRepo;

    @Inject
    RuleEngineService ruleEngineService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MockCbiAuthService mockCbiAuthService; 

    @Inject
    CredemAccountService credemAccountService;

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    // --- HEADER E VALORI MOCKATI ---
    // Questi valori saranno usati per le chiamate al CredemClient
    private static final String MOCK_DIGEST = "SHA-256=MOCK_DIGEST_VALUE";
    private static final String MOCK_SIGNATURE = "MOCK_SIGNATURE_VALUE";
    private static final String MOCK_TPP_CERTIFICATE = "MOCK_TPP_CERTIFICATE_BASE64";
    private static final String MOCK_PSU_IP_ADDRESS = "127.0.0.1";
    private static final String MOCK_ASPSP_CODE = "MOCK_ASPSP_CODE";
    

    @ConfigProperty(name = "credem.psu-id")
    String psuId;

    @ConfigProperty(name = "scheduler.load-from-file", defaultValue = "true")
    boolean loadFromFile;

    @ConfigProperty(name = "scheduler.test-data.filenames")
    List<String> testDataFilenames;

    @ConfigProperty(name = "credem.account-id")
    String defaultAccountIdForOperations;

    @ConfigProperty(name = "cbi.consent-mock-id")
    String mockConsentId;

    // --- Metodi di Business Logic ---

    /**
     * Scarica e salva i movimenti delle transazioni.
     * Se la proprietà 'scheduler.load-from-file' è true, carica le transazioni da file di test.
     * Altrimenti, effettua chiamate paginated all'API Credem per recuperare i movimenti
     * e li salva nel database.
     *
     * @param accountId L'ID dell'account per cui scaricare i movimenti.
     * @param from La data di inizio del periodo di scaricamento (formato YYYY-MM-DD).
     * @param to La data di fine del periodo di scaricamento (formato YYYY-MM-DD).
     */
    @Transactional
    public void downloadAndSave(String accountId, String from, String to) {
        log.info("Inizio scaricamento e memorizzazione movimenti per accountId: {} da {} a {}.", accountId, from, to);

        if (loadFromFile) {
            log.info("Caricamento movimenti da file di test: {}", (Object) testDataFilenames);
            if (testDataFilenames == null || testDataFilenames.isEmpty()) {
                log.warn("Nessun file di test specificato per il caricamento dei movimenti.");
                return;
            }
            for (String filename : testDataFilenames) {
                try {
                    loadFromFile(filename, accountId);
                } catch (IOException e) {
                    log.error("Errore durante il caricamento del file {}: {}", (Object) filename, (Object) e.getMessage());
                }
            }
            log.info("Caricamento da file completato.");
            return;
        }

        int offset = 0;
        int limit = 100;
        boolean hasMore = true;

        String token = mockCbiAuthService.getAccessToken(); // Ottengo il token Bearer mockato
        String xRequestId = "req-" + UUID.randomUUID().toString();
        String dateHeader = OffsetDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMATTER);
        String psuAuthorization = mockCbiAuthService.getPsuAccessToken(psuId, mockConsentId);

        while (hasMore) {
            log.info("Scaricamento pagina con offset: {}", (Object) offset);

            try {
                CredemTransactionResponse response = credemClient.getTransactions(
                        psuId,
                        token,
                        xRequestId,
                        mockConsentId,
                        dateHeader,
                        MOCK_DIGEST, // Uso il digest mockato
                        MOCK_SIGNATURE, // Uso la signature mockata
                        MOCK_TPP_CERTIFICATE, // Uso il certificato TPP mockato
                        psuAuthorization,
                        MOCK_PSU_IP_ADDRESS, // Uso l'IP mockato
                        MOCK_ASPSP_CODE, // Uso il codice ASPSP mockato
                        accountId,
                        from,
                        to,
                        limit,
                        offset);

                if (response != null && response.booked != null && !response.booked.isEmpty()) {
                    saveTransactionsFromDTOList(response.booked, accountId);
                    offset += limit;
                    hasMore = response.booked.size() >= limit;
                } else {
                    hasMore = false;
                }
            } catch (Exception e) {
                log.error("Errore durante la chiamata all'API Credem per offset {}: {}", (Object) offset, (Object) e.getMessage(), e);
                hasMore = false;
            }
        }
        log.info("Scaricamento movimenti completato da {} a {}.", (Object) from, (Object) to);
    }

    /**
     * Salva una lista di oggetti TransactionData (DTO) nel database come entità Transaction.
     * Applica la logica di classificazione e previene la persistenza di transazioni duplicate
     * basandosi sul transactionId.
     *
     * @param dtoList La lista di DTO delle transazioni da salvare.
     * @param defaultAccountId L'ID dell'account da assegnare se il DTO non lo specifica.
     */
    @Transactional
    public void saveTransactionsFromDTOList(List<TransactionData> dtoList, String defaultAccountId) {
        log.info("Inizio salvataggio movimenti da lista. Transazioni da processare: {}",
                (Object) (dtoList != null ? dtoList.size() : 0));
        if (dtoList == null || dtoList.isEmpty()) {
            return;
        }

        for (TransactionData dto : dtoList) {
            Transaction transaction = fromDto(dto);

            if (transaction.getAccountId() == null || transaction.getAccountId().isBlank()) {
                transaction.setAccountId(defaultAccountId);
                log.warn("Transaction con transactionId {} non aveva accountId nel DTO, assegnato: {}",
                        (Object) transaction.getTransactionId(), (Object) defaultAccountId);
            }

            ruleEngineService.classifyTransaction(transaction);

            if (transactionRepo.find("transactionId", transaction.getTransactionId()).firstResultOptional().isEmpty()) {
                transactionRepo.persist(transaction);
                log.debug("Persistito transaction con ID transazione: {}", (Object) transaction.getTransactionId());
            } else {
                log.debug("Transaction già presente con ID transazione: {}", (Object) transaction.getTransactionId());
            }
        }
        log.info("Salvataggio movimenti completato.");
    }

    /**
     * Converte un oggetto TransactionData (DTO) in un'entità Transaction.
     * Effettua il parsing delle date e imposta l'importo e la valuta.
     *
     * @param dto Il DTO della transazione da convertire.
     * @return L'entità Transaction popolata con i dati del DTO.
     */
    private Transaction fromDto(TransactionData dto) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(dto.transactionId);

        if (dto.bookingDate != null) {
            transaction.setBookingDate(OffsetDateTime.parse(dto.bookingDate, ISO_FORMATTER));
        }
        if (dto.valueDate != null) {
            transaction.setValueDate(OffsetDateTime.parse(dto.valueDate, ISO_FORMATTER));
        }

        if (dto.transactionAmount != null) {
            transaction.setAmount(dto.transactionAmount.amount);
            transaction.setCurrency(dto.transactionAmount.currency.name());
        }

        transaction.setRemittanceInformation(dto.remittanceInformationUnstructured);
        transaction.setCreditorName(dto.creditorName);
        transaction.setDebtorName(dto.debtorName);
        transaction.setBankTransactionCode(dto.bankTransactionCode);
        transaction.setProprietaryBankTransactionCode(dto.proprietaryBankTransactionCode);
        transaction.setAdditionalInformation(dto.additionalInformation);

        transaction.setAccountId(dto.accountId);

        return transaction;
    }

    /**
     * Ricerca le transazioni nel database basandosi su vari criteri.
     *
     * @param accountId L'ID dell'account per cui cercare le transazioni.
     * @param fromDate La data di inizio del periodo di ricerca (inclusa).
     * @param toDate La data di fine del periodo di ricerca (esclusa).
     * @param centerType Il tipo di centro di costo/ricavo per filtrare le transazioni.
     * @return Una lista di transazioni che corrispondono ai criteri di ricerca.
     */
    public List<Transaction> searchTransactions(
            String accountId,
            LocalDate fromDate,
            LocalDate toDate,
            CenterType centerType) {

        StringBuilder queryBuilder = new StringBuilder("accountId = :accountId");
        Parameters parameters = Parameters.with("accountId", accountId);

        OffsetDateTime fromOffsetDateTime = null;
        OffsetDateTime toOffsetDateTime = null;

        if (fromDate != null) {
            fromOffsetDateTime = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        }
        if (toDate != null) {
            toDate = toDate.plusDays(1);
            toOffsetDateTime = toDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        }

        if (fromOffsetDateTime != null && toOffsetDateTime != null) {
            queryBuilder.append(" and bookingDate >= :fromBookingDate AND bookingDate < :toBookingDate");
            parameters.and("fromBookingDate", fromOffsetDateTime).and("toBookingDate", toOffsetDateTime);
        }

        if (centerType != null) {
            queryBuilder.append(" and centerType = :centerType");
            parameters.and("centerType", centerType);
        }

        return transactionRepo.find(queryBuilder.toString(), parameters).list();
    }

    
    //Carica i movimenti delle transazioni da un file JSON locale.
    
    //forse meglio spostare anche questo?? chiedere!
    @Transactional
    public void loadFromFile(String filename, String targetAccountId) throws IOException {
        log.info("Tentativo di importare movimenti dal file: {}", (Object) filename);
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

        saveTransactionsFromDTOList(response.booked, targetAccountId);

        log.info("Importazione dal file {} completata. Movimenti salvati.", (Object) filename);

    }

}
