package it.coderit.banktestapp.service;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.coderit.banktestapp.dto.Rule;
import it.coderit.banktestapp.model.Transaction;
import it.coderit.banktestapp.model.ClassificationRule;
import it.coderit.banktestapp.model.CenterType;

import it.coderit.banktestapp.repository.ClassificationRuleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class RuleEngineService {

    @Inject
    ClassificationRuleRepository regolaClassificazioneRepository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public void classifyTransaction(Transaction transaction) {
        List<ClassificationRule> rules = regolaClassificazioneRepository.findAllRegole();

        for (ClassificationRule regola : rules) {
            try {
                Rule rule = objectMapper.readValue(regola.getJsonRule(), Rule.class);

                boolean matchesAll = rule.conditions.stream().allMatch(cond -> {
                    String fieldValue = extractFieldValue(transaction, cond.field);
                    if (fieldValue == null)
                        return false;
                    return cond.keywords.stream()
                            .anyMatch(kw -> fieldValue.toLowerCase().contains(kw.toLowerCase()));
                });

                if (matchesAll) {
                    transaction.setCenterType(regola.getCenterType());
                    return;
                }
            } catch (Exception e) {
                System.err.println("Errore parsing json_rule per regola id=" + regola.getId() + ": " + e.getMessage());
            }
        }

        // Nessuna regola soddisfatta -> center di default
        transaction.setCenterType(CenterType.COSTO);
        System.err.println("Nessuna regola applicata, applicato center di default COSTO");
    }

    private String extractFieldValue(Transaction transaction, String field) {
        switch (field) {
            case "remittanceInformationUnstructured":
                return transaction.getRemittanceInformation();
            case "DebtorName":
                return transaction.getDebtorName();
            case "CreditorName":
                return transaction.getCreditorName();
            case "bankTransactionCode":
                return transaction.getBankTransactionCode();
            case "propietaryBankTransactionCode":
                return transaction.getProprietaryBankTransactionCode();
            default:
                return null;
        }
    }

}
