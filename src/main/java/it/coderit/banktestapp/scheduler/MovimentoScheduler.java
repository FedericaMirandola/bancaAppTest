package it.coderit.banktestapp.scheduler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import io.quarkus.scheduler.Scheduled;
import it.coderit.banktestapp.service.MovimentoService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MovimentoScheduler {
    
    @Inject
    MovimentoService movimentoService;

    @ConfigProperty(name = "scheduler.daysback", defaultValue = "1")
    int giorniIndietro;

    @ConfigProperty(name = "credem.account-id")
    String accountId;

    @Scheduled(cron = "{movimenti.scaricamento.cron}")
    void scaricaPeriodicamente() {
        LocalDate oggi = LocalDate.now();
        LocalDate inizio = oggi.minusDays(giorniIndietro);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String from = inizio.format(formatter);
        String to = oggi.format(formatter);

        movimentoService.scaricaEMemorizzaMovimenti(accountId, from, to);
    }
}

