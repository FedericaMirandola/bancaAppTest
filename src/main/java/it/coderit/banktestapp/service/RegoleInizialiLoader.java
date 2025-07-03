package it.coderit.banktestapp.service;

import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.model.CenterType;
import it.coderit.banktestapp.repository.ClassificationRuleRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RegoleInizialiLoader {

    private static final Logger log = LoggerFactory.getLogger(RegoleInizialiLoader.class);

    @Inject
    ClassificationRuleRepository regolaRepo;

    void onStart(@Observes StartupEvent ev) {
        log.info("RegoleInizialiLoader: Avvio dell'inizializzazione delle regole dopo il completo startup dell'applicazione.");
        initializeRules();
    }

    @Transactional
    void initializeRules() {
        if (regolaRepo.count() == 0) {
            try {
                log.info("Inizializzazione delle regole: Inserimento regole di default.");

                // Regole per COSTO
                logAndSaveRule("supermercato", CenterType.COSTO);
                logAndSaveRule("bolletta", CenterType.COSTO);
                logAndSaveRule("affitto", CenterType.COSTO);
                logAndSaveRule("acquisto", CenterType.COSTO);
                logAndSaveRule("carburante", CenterType.COSTO);

                // Regole per PROFITTO
                logAndSaveRule("stipendio", CenterType.PROFITTO);
                logAndSaveRule("bonifico", CenterType.PROFITTO);
                logAndSaveRule("vendita", CenterType.PROFITTO);
                logAndSaveRule("deposito", CenterType.PROFITTO);
                logAndSaveRule("consulting", CenterType.PROFITTO);
                logAndSaveRule("fattura", CenterType.PROFITTO);

                log.info("Regole iniziali inserite correttamente.");
            } catch (Exception e) {
                log.error("Errore durante l'inizializzazione delle rules", e);
                throw new RuntimeException("Errore durante l'inizializzazione delle rules", e);
            }
        } else {
            log.info("Regole giÃ  presenti, nessuna inizializzazione necessaria.");
        }
    }

    private void logAndSaveRule(String keyword, CenterType centerType) {
        if (centerType == null) {
            log.error("Tentativo di salvare regola con keyword '{}' ma CenterType Ã¨ NULL. Questa regola non verrÃ  salvata.", keyword);
            return; 
        }
        log.debug("Preparazione salvataggio regola: Keyword='{}', CenterType={}", keyword, centerType);
        regolaRepo.saveIfNotExists(keyword, centerType);
    }
}
