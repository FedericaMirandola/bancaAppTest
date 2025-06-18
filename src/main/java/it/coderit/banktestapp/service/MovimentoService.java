// File: it.coderit.banktestapp.service.MovimentoService

package it.coderit.banktestapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.coderit.banktestapp.dto.CredemTransactionResponse;
import it.coderit.banktestapp.dto.CredemTransactionResponse.TransactionData;
import it.coderit.banktestapp.model.Movimento;
import it.coderit.banktestapp.model.TipoCentro;

import it.coderit.banktestapp.repository.MovimentoRepository;
import it.coderit.banktestapp.repository.RegolaClassificazioneRepository;
import it.coderit.banktestapp.rest.CredemClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MovimentoService {

    @Inject
    @RestClient // Questo inietta automaticamente il FakeCredemClient o il client reale in base alla configurazione
    CredemClient credemClient;

    @Inject
    RegolaClassificazioneRepository regolaRepo;

    @Inject
    MovimentoRepository movimentoRepo;

    @Inject
    RuleEngineService ruleEngineService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MockCbiAuthService mockCbiAuthService;

    private static final Logger log = LoggerFactory.getLogger(MovimentoService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    
    String accountId;

    @ConfigProperty(name = "credem.psu-id")
    String psuId;

    @Transactional
    public void scaricaEMemorizzaMovimenti(String accountId,String from, String to) {
        int offset = 0;
        int limit = 100;
        boolean hasMore = true;

        String token = mockCbiAuthService.getAccessToken(); // Ottengo il token Bearer mockato

        while (hasMore) {
            log.info("Scaricamento pagina con offset: {}", offset);

            // Questa chiamata userà il FakeCredemClient se use.fake.credem=true
            // o il client reale altrimenti.
            CredemTransactionResponse response = credemClient.getTransactions(
                    accountId,
                    from,
                    to,
                    limit,
                    offset,
                    psuId,
                    token);

            if (response != null && response.booked != null && !response.booked.isEmpty()) {
                salvaMovimentiDaLista(response.booked);
                offset += limit;
                hasMore = response.booked.size() >= limit;
            } else {
                hasMore = false;
            }
        }
        log.info("Scaricamento movimenti completato da {} a {}.", from, to);
    }

    @Transactional
    public List<Movimento> leggiMovimenti(String from, String to) {
        if (from == null || to == null) {
            return movimentoRepo.listAll();
        }

        OffsetDateTime fromDate = LocalDate.parse(from).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDate = LocalDate.parse(to).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        return movimentoRepo.find("bookingDate >= ?1 AND bookingDate <= ?2", fromDate, toDate).list();
    }

    @Transactional
    public void salvaMovimentiDaLista(List<TransactionData> dtoList) {
        for (TransactionData dto : dtoList) {
            Movimento movimento = fromDto(dto);
            ruleEngineService.classifyMovimento(movimento);

            // Evita duplicati controllando transactionId
            if (movimentoRepo.find("transactionId", movimento.getTransactionId())
                    .firstResult() == null) {
                movimentoRepo.persist(movimento);
                log.debug("Persistito movimento con ID transazione: {}", movimento.getTransactionId());
            } else {
                log.debug("Movimento già presente con ID transazione: {}", movimento.getTransactionId());
            }
        }
    }

    private Movimento fromDto(TransactionData dto) {
        Movimento movimento = new Movimento();
        movimento.setTransactionId(dto.transactionId);
        if (dto.bookingDate != null) {
            movimento.setBookingDate(OffsetDateTime.parse(dto.bookingDate, ISO_FORMATTER));
        }
        if (dto.valueDate != null) {
            movimento.setValueDate(OffsetDateTime.parse(dto.valueDate, ISO_FORMATTER));
        }
        if (dto.transactionAmount != null) {
            movimento.setAmount(dto.transactionAmount.amount);
            movimento.setCurrency(dto.transactionAmount.currency.name());
        }
        movimento.setRemittanceInformation(dto.remittanceInformationUnstructured);
        movimento.setCreditorName(dto.creditorName);
        movimento.setDebtorName(dto.debtorName);
        movimento.setBankTransactionCode(dto.bankTransactionCode);
        movimento.setProprietaryBankTransactionCode(dto.proprietaryBankTransactionCode);

        if (dto.accountId != null && !dto.accountId.isBlank()) {
            movimento.setAccountId(dto.accountId);
        } else {
            movimento.setAccountId(accountId);
        }

        return movimento;
    }

    @Transactional
    public List<Movimento> leggiMovimentiPerAccountIdEData(String accountId, String from, String to) {
        // Valida i parametri di data
        if (from == null || to == null) {
            // Potresti voler lanciare un'eccezione o gestire un caso di errore qui
            log.warn("Parametri di data 'from' o 'to' nulli in leggiMovimentiPerAccountIdEData.");
            return List.of(); // Ritorna lista vuota o lancia eccezione
        }

        OffsetDateTime fromDate = LocalDate.parse(from).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDate = LocalDate.parse(to).atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        // Delega al repository per filtrare anche per accountId
        return movimentoRepo.findByAccountIdAndDateRange(accountId, fromDate, toDate);
    }

    // Il metodo importaDaFile rimane nel service, ma non è più chiamato da PostConstruct
    // e non è esposto direttamente dal controller.
    // Può essere utile per un test manuale o se hai un'altra logica interna che lo richiede.
    @Transactional
    public List<Movimento> importaDaFile(String filename) throws IOException {
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

        salvaMovimentiDaLista(response.booked);

        return List.of();
    }

    public List<Movimento> leggiMovimentiPerTipoCentro(TipoCentro tipoCentro) {
        return movimentoRepo.findByCentroTipo(tipoCentro);
    }
}