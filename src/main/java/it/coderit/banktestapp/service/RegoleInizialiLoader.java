    package it.coderit.banktestapp.service;

    import it.coderit.banktestapp.model.ClassificationRule;
    import it.coderit.banktestapp.model.CenterType;
    import it.coderit.banktestapp.repository.ClassificationRuleRepository;

    import jakarta.enterprise.context.ApplicationScoped;
    import jakarta.enterprise.event.Observes;
    import jakarta.inject.Inject;
    import jakarta.transaction.Transactional;

    import java.util.List;
    import java.util.Map;

    import com.fasterxml.jackson.databind.ObjectMapper;

    import io.quarkus.runtime.StartupEvent;

    @ApplicationScoped
    public class RegoleInizialiLoader {

        @Inject
        ClassificationRuleRepository regolaRepo;

        @Inject
        ObjectMapper objectMapper;

        void onStart(@Observes StartupEvent ev) {
            System.out.println("RegoleInizialiLoader: Avvio dell'inizializzazione delle regole dopo il completo startup dell'applicazione.");
            inizializzaRegole();
        }

        @Transactional
        void inizializzaRegole() {
            if (regolaRepo.count() == 0) {
                try {
                    String jsonCosto = objectMapper.writeValueAsString(
                            Map.of("keywords", List.of("supermarket", "bolletta", "affitto", "acquisto")));

                    String jsonProfitto = objectMapper.writeValueAsString(
                            Map.of("keywords", List.of("fattura", "bonifico", "vendita", "deposito", "Consulting")));

                    ClassificationRule regolaCosto = new ClassificationRule();
                    regolaCosto.setCenter(CenterType.COSTO);
                    regolaCosto.setJsonRule(jsonCosto);
                    regolaCosto.setKeyword("costo");

                    ClassificationRule regolaProfitto = new ClassificationRule();
                    regolaProfitto.setCenter(CenterType.PROFITTO);
                    regolaProfitto.setJsonRule(jsonProfitto);
                    regolaProfitto.setKeyword("profitto");

                    regolaRepo.persist(regolaCosto);
                    regolaRepo.persist(regolaProfitto);

                    System.out.println("Regole iniziali inserite correttamente.");
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Errore durante l'inizializzazione delle rules", e);
                }
            } else {
                System.out.println("Regole già presenti, nessuna inizializzazione necessaria.");
            }
        }
    }
    