package it.coderit.banktestapp.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

    /**
     * Trova il CenterType corrispondente a una transazione cercando le parole chiave delle regole
     * nei campi rilevanti della transazione (remittance information, creditor/debtor name, proprietary bank transaction code).
     *
     * @param transaction La transazione da classificare.
     * @return Un Optional contenente il CenterType se una regola corrisponde, altrimenti un Optional vuoto.
     */
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

    /**
     * Salva una nuova regola di classificazione se una regola con la stessa parola chiave (case-insensitive)
     * non esiste già.
     *
     * @param keyword La parola chiave della regola.
     * @param center Il CenterType associato alla parola chiave.
     */
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

    /**
     * Trova una regola di classificazione per la sua parola chiave.
     *
     * @param keyword La parola chiave da cercare.
     * @return Un Optional contenente la ClassificationRule se trovata, altrimenti un Optional vuoto.
     */
    public Optional<ClassificationRule> findByKeyword(String keyword) {
        return find("LOWER(keyword) = ?1", keyword.toLowerCase()).firstResultOptional();
    }


    /**
     * Restituisce tutte le regole di classificazione presenti nel database.
     *
     * @return Una lista di tutte le ClassificationRule.
     */
    public List<ClassificationRule> findAllRegole() {
        return listAll();
    }
}
