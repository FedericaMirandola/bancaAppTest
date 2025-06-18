package it.coderit.banktestapp.service;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.coderit.banktestapp.dto.Regola;
import it.coderit.banktestapp.model.Movimento;
import it.coderit.banktestapp.model.RegolaClassificazione;
import it.coderit.banktestapp.model.TipoCentro;

import it.coderit.banktestapp.repository.RegolaClassificazioneRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RuleEngineService {

    @Inject
    RegolaClassificazioneRepository regolaClassificazioneRepository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public void classifyMovimento(Movimento movimento) {
        List<RegolaClassificazione> regole = regolaClassificazioneRepository.findAllRegole();

        for (RegolaClassificazione regola : regole) {
            try {
                Regola rule = objectMapper.readValue(regola.getJsonRule(), Regola.class);

                boolean matchesAll = rule.conditions.stream().allMatch(cond -> {
                    String fieldValue = extractFieldValue(movimento, cond.field);
                    if (fieldValue == null)
                        return false;
                    return cond.keywords.stream()
                            .anyMatch(kw -> fieldValue.toLowerCase().contains(kw.toLowerCase()));
                });

                if (matchesAll) {
                    movimento.setTipoCentro(regola.getTipoCentro());
                    return;
                }
            } catch (Exception e) {
                System.err.println("Errore parsing json_rule per regola id=" + regola.getId() + ": " + e.getMessage());
            }
        }

        // Nessuna regola soddisfatta -> centro di default
        movimento.setTipoCentro(TipoCentro.COSTO);
        System.err.println("Nessuna regola applicata, applicato centro di default COSTO");
    }

    private String extractFieldValue(Movimento movimento, String field) {
        switch (field) {
            case "remittanceInformationUnstructured":
                return movimento.getRemittanceInformation();
            case "DebtorName":
                return movimento.getDebtorName();
            case "CreditorName":
                return movimento.getCreditorName();
            case "bankTransactionCode":
                return movimento.getBankTransactionCode();
            case "propietaryBankTransactionCode":
                return movimento.getProprietaryBankTransactionCode();
            default:
                return null;
        }
    }

}
