package it.coderit.banktestapp.repository;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.model.CenterType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class ClassificationRuleRepository implements PanacheRepository<ClassificationRule> {

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper objectMapper;

    public Optional<CenterType> findCentroByParolaChiave(Transaction transaction) {
        List<ClassificationRule> tutteLeRegole = listAll();

        List<String> valoriDaCercare = List.of(
                transaction.getRemittanceInformation(),
                transaction.getCreditorName(),
                transaction.getDebtorName(),
                transaction.getProprietaryBankTransactionCode(),
                transaction.getBankTransactionCode())
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(String::toLowerCase)
                .toList();

        for (ClassificationRule regola : tutteLeRegole) {
            String jsonRule = regola.getJsonRule();
            if (jsonRule == null || jsonRule.isEmpty()) {
                continue;
            }

            try {
                JsonNode root = objectMapper.readTree(jsonRule);
                JsonNode keywordsNode = root.get("keywords");

                if (keywordsNode != null && keywordsNode.isArray()) {
                    for (JsonNode keywordNode : keywordsNode) {
                        String keyword = keywordNode.asText().toLowerCase();

                        for (String valore : valoriDaCercare) {
                            if (valore.contains(keyword)) {
                                return Optional.of(regola.getCenter());
                            }
                        }
                    }
                }
            } catch (Exception e) {
            
                e.printStackTrace();
            }
        }

        return Optional.empty();
    }

    public void saveIfNotExists(String keyword, CenterType center) {
        boolean exist = find("LOWER(keyword) = ?1", keyword.toLowerCase()).firstResultOptional().isPresent();
        if (!exist) {
            ClassificationRule regola = new ClassificationRule();
            regola.setKeyword(keyword.toLowerCase());
            regola.setCenter(center);
            persist(regola);
        }
    }

    public List<ClassificationRule> findAllRegole() {
        return getEntityManager().createQuery("FROM ClassificationRule", ClassificationRule.class)
                .getResultList();
    }

}
