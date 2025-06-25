package it.coderit.banktestapp.dto;

import it.coderit.banktestapp.dto.CredemAccountResponse.BalanceData;
import it.coderit.banktestapp.dto.CredemAccountResponse.ErrorMenagement;
import it.coderit.banktestapp.dto.CredemAccountResponse.TppMessage;

import java.util.List;

public class CredemBalancesResponse {

    public BalancesAccountInfo account;

    public List<BalanceData> balances;

    public ErrorMenagement errorManagement;
    public List<TppMessage> tppMessages;

    public static class BalancesAccountInfo {
        
        public String iban;
        public String bban;
        public String pan;
        public String maskedPan;
        public String msisdn;
        public String currency;

        // Aggiungi altri campi se necessario
    }
}