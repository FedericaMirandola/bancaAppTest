package it.coderit.banktestapp.dto;

import it.coderit.banktestapp.dto.CredemAccountResponse.AccountBalance;
import it.coderit.banktestapp.dto.CredemAccountResponse.AccountBalance;
import it.coderit.banktestapp.dto.CredemAccountResponse.ErrorManagement;
import it.coderit.banktestapp.dto.CredemAccountResponse.TppMessage;

import java.util.List;

public class CredemBalancesResponse {

    public BalancesAccountInfo account;

    public List<AccountBalance> balances;

    public ErrorManagement errorManagement;
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