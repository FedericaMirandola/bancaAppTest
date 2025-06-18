package it.coderit.banktestapp.repository;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import it.coderit.banktestapp.model.Movimento;
import it.coderit.banktestapp.model.RegolaClassificazione;
import it.coderit.banktestapp.model.TipoCentro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class RegolaClassificazioneRepository implements PanacheRepository<RegolaClassificazione> {

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper objectMapper;

    public Optional<TipoCentro> findCentroByParolaChiave(Movimento movimento) {
        List<RegolaClassificazione> tutteLeRegole = listAll();

        List<String> valoriDaCercare = List.of(
                movimento.getRemittanceInformation(),
                movimento.getCreditorName(),
                movimento.getDebtorName(),
                movimento.getProprietaryBankTransactionCode(),
                movimento.getBankTransactionCode())
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(String::toLowerCase)
                .toList();

        for (RegolaClassificazione regola : tutteLeRegole) {
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
                                return Optional.of(regola.getCentro());
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

    public void saveIfNotExists(String parolaChiave, TipoCentro centro) {
        boolean exist = find("LOWER(parolaChiave) = ?1", parolaChiave.toLowerCase()).firstResultOptional().isPresent();
        if (!exist) {
            RegolaClassificazione regola = new RegolaClassificazione();
            regola.setParolaChiave(parolaChiave.toLowerCase());
            regola.setCentro(centro);
            persist(regola);
        }
    }

    public List<RegolaClassificazione> findAllRegole() {
        return getEntityManager().createQuery("FROM RegolaClassificazione", RegolaClassificazione.class)
                .getResultList();
    }

}
