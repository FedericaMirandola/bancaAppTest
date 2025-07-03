package it.coderit.banktestapp.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.model.CenterType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClassificationRuleRepository implements PanacheRepository<ClassificationRule> {

    private static final Logger log = LoggerFactory.getLogger(ClassificationRuleRepository.class);

    @Inject
    EntityManager em;

    public Optional<CenterType> findCenterByJeyWord(Transaction transaction) {
        List<ClassificationRule> allRules = listAll();

        List<String> transactionFieldsToSerach = Stream.of(
                transaction.getRemittanceInformation(),
                transaction.getCreditorName(),
                transaction.getDebtorName(),
                transaction.getProprietaryBankTransactionCode())
                .filter(java.util.Objects::nonNull)
                .map(String::toLowerCase)
                .toList();
        log.debug("Valori della transazione da cercare: {}", transactionFieldsToSerach);

        for (ClassificationRule rule : allRules) {
            String ruleKeyword = rule.getKeyword().toLowerCase();
            log.debug("Valutazione regola: ID={}, Keyword='{}', CenterType={}", rule.getId(),
                    rule.getKeyword(), rule.getCenterType());

            boolean keywordFound = transactionFieldsToSerach.stream()
                    .anyMatch(fieldValue -> fieldValue.contains(ruleKeyword));

            if (keywordFound) {
                log.info("Regola ID={} con keyword '{}' ha trovato corrispondenza. Assegnato CenterType: {}",
                        rule.getId(), rule.getKeyword(), rule.getCenterType());
                return Optional.of(rule.getCenterType());
            }
        }

        log.info("Nessuna regola di classificazione ha trovato corrispondenza per la transazione.");
        return Optional.empty();
    }

    public void saveIfNotExists(String keyword, CenterType center) {
        boolean exist = find("LOWER(keyword) = ?1", keyword.toLowerCase()).firstResultOptional().isPresent();
        if (!exist) {
            ClassificationRule rule = new ClassificationRule(keyword, center);
            persist(rule);
            log.info("Creata nuova regola di classificazione: Keyword='{}', CenterType={}", keyword, center);
        } else {
            log.info("Regola con keyword '{}' esistente, nessuna creazione.", keyword);
        }
    }

    public List<ClassificationRule> findAllRegole() {
        return listAll();
    }

}
