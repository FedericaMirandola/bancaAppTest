package it.coderit.banktestapp.service;

import it.coderit.banktestapp.model.RegolaClassificazione;
import it.coderit.banktestapp.model.TipoCentro;
import it.coderit.banktestapp.repository.RegolaClassificazioneRepository;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class RegoleInizialiLoader {

    @Inject
    RegolaClassificazioneRepository regolaRepo;

    @Inject
    ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        inizializzaRegole();
    }

    @Transactional
    void inizializzaRegole() {
        if (regolaRepo.count() == 0) {
            try {
                // Inizializza le regole di classificazione con JSON
                String jsonCosto = objectMapper.writeValueAsString(
                        Map.of("keywords", List.of("supermarket", "bolletta", "affitto", "acquisto")));

                String jsonProfitto = objectMapper.writeValueAsString(
                        Map.of("keywords", List.of("fattura", "bonifico", "vendita", "deposito", "Consulting")));

                RegolaClassificazione regolaCosto = new RegolaClassificazione();
                regolaCosto.setCentro(TipoCentro.COSTO);
                regolaCosto.setJsonRule(jsonCosto);
                regolaCosto.setParolaChiave("costo");

                RegolaClassificazione regolaProfitto = new RegolaClassificazione();
                regolaProfitto.setCentro(TipoCentro.PROFITTO);
                regolaProfitto.setJsonRule(jsonProfitto);
                regolaProfitto.setParolaChiave("profitto");

                regolaRepo.persist(regolaCosto);
                regolaRepo.persist(regolaProfitto);

                System.out.println("Regole iniziali inserite correttamente.");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Errore durante l'inizializzazione delle regole", e);
            }
        } else {
            System.out.println("Regole già presenti, nessuna inizializzazione necessaria.");
        }
    }
}