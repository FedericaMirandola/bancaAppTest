package it.coderit.banktestapp.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.model.CenterType;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClassificationRuleRepository implements PanacheRepository<ClassificationRule> {

    private static final Logger log = LoggerFactory.getLogger(ClassificationRuleRepository.class);

    @Inject
    EntityManager em;

    @ConfigProperty(name = "classification.transaction-fields")
    List<String> configuratedFields;

    private Map<String, Function<Transaction, String>> fieldsGetters;


    @PostConstruct
    void init() {
        fieldsGetters = new HashMap<String, Function<Transaction, String>>();
        fieldsGetters.put("remittanceInformation", Transaction::getRemittanceInformation);
        fieldsGetters.put("creditorName", Transaction::getCreditorName);
        fieldsGetters.put("debtorName", Transaction::getDebtorName);
        fieldsGetters.put("additionalInformation", Transaction::getAdditionalInformation);
    }

    /**
     * Trova il CenterType corrispondente a una transazione cercando le parole chiave delle regole
     * nei campi rilevanti della transazione (remittance information, creditor/debtor name, proprietary bank transaction code).
     *
     * @param transaction La transazione da classificare.
     * @return Un Optional contenente il CenterType se una regola corrisponde, altrimenti un Optional vuoto.
     */
    public Optional<CenterType> findCenterByKeyword(Transaction transaction) {
        List<ClassificationRule> allRules = listAll();

        List<String> transactionFieldsToSerach = configuratedFields.stream()
        .map(fieldName -> {
            Function<Transaction, String> getter = fieldsGetters.get(fieldName);
            if (getter != null) {
                return getter.apply(transaction);
            }
            log.warn("Campo transazione {} configurato ma getter non trovato", fieldName);
            return null;
        }).filter(s -> s != null).map(String::toLowerCase).collect(Collectors.toList());

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
     */
    public Optional<ClassificationRule> findByKeyword(String keyword) {
        return find("LOWER(keyword) = ?1", keyword.toLowerCase()).firstResultOptional();
    }


    /**
     * Restituisce tutte le regole di classificazione presenti nel database.
     */
    public List<ClassificationRule> findAllRegole() {
        return listAll();
    }
}
