package it.coderit.banktestapp.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.quarkus.scheduler.Scheduled;
import it.coderit.banktestapp.service.TransactionService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransactionScheduler.class);

    @Inject
    TransactionService transactionService;

    @ConfigProperty(name = "scheduler.daysback", defaultValue = "1")
    Integer daysBack;

    @ConfigProperty(name = "credem.account-id")
    String accountId;

    @ConfigProperty(name = "scheduler.enabled", defaultValue = "false")
    boolean schedulerEnabled;

    /**
     * Download dei movimenti eseguito una volta sola all'avvio
     */
    @PostConstruct
    void loadOnceAtStartup() {
        log.info("Esecuzione unica iniziale del download movimenti all'avvio dell'app.");
        loadTransactions();
    }

    /**
     * Download ciclico secondo cron, abilitato solo se scheduler.enabled = true
     */
    @Scheduled(delayed = "5s", cron = "{transaction.scaricamento.cron}")
    void scaricaPeriodicamente() {
        if (schedulerEnabled) {
            log.info("Esecuzione periodica abilitata da scheduler.enabled=true");
            loadTransactions();
        } else {
            log.info("Esecuzione periodica disabilitata (scheduler.enabled=false)");
        }
    }

    private void loadTransactions() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(daysBack);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String from = start.format(formatter);
        String to = today.format(formatter);

        try {
            log.info("Download movimenti per accountId={} da {} a {}", accountId, from, to);
            transactionService.downloadAndSave(accountId, from, to);
            log.info("Movimenti scaricati e salvati correttamente.");
        } catch (Exception e) {
            log.error("Errore durante lo scaricamento/salvataggio movimenti: {}", e.getMessage(), e);
        }
    }
}
