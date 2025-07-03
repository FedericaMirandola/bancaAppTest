package it.coderit.banktestapp.service;

import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.CenterType;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.repository.ClassificationRuleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);

    @Inject
    ClassificationRuleRepository regolaClassificazioneRepository;


    @Transactional
    public void classifyTransaction(Transaction transaction) {
        log.info("Tentativo di classificare la transazione con ID: {}", transaction.getTransactionId());

        if(Boolean.TRUE.equals(transaction.getIsManuallyClassified())) {
            log.debug("Transazione {} classificata manualmente.Salta la classificazione automatica!", transaction.getTransactionId());
            return;
        }
        regolaClassificazioneRepository.findCenterByJeyWord(transaction)
        .ifPresentOrElse(
            //se una regola matcha il centerType allora la assegna ad un centro
            matchedCenterType -> {
                transaction.setCenterType(matchedCenterType);
                log.info("Transazione ID: {} classificata come: {}", transaction.getTransactionId());
            },
            //se nessuna regola matcha il centerType allora laassegna ad UNDEFINED
            () -> {
                transaction.setCenterType(CenterType.UNDEFINED);
            }
        );
    }

}
