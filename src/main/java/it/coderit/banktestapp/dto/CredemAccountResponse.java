package it.coderit.banktestapp.dto;

import java.util.List;
import java.math.BigDecimal;
import java.time.OffsetDateTime; 
import java.util.Map;



//  DTO (Data Transfer Object) per la risposta dell'API Credem che elenca gli account.

public class CredemAccountResponse {
    // La lista di oggetti AccountData, che rappresentano i singoli account disponibili.
    // Il nome del campo 'accounts' deve corrispondere esattamente alla chiave JSON.
    public List<AccountData> accounts;


    public static class AccountData {
        
        public String resourceId;
        public String iban;
        public String currency;
        public String bban;
        public String pan;
        public String maskedPan;
        public String msisdn;
        public String name;
        public String product;
        public String cashAccountType;
        public String bic;
        public String linkedAccounts;
        public String usage;
        public String details;

        public List<AccountBalance> balances; // Lista di saldi associati all'account
        public Map<String, LinkData> _links;
        public ErrorManagement errorManagement;
        public List<TppMessage> tppMessages;

        
    }

    public static class AccountBalance {
        public BigDecimal amount; 
        public String currency; 
        public String balanceType;
        public OffsetDateTime lastChangeDateTime; 
        public String referenceDate;
        public String lastCommittedTransactionEntry;

    }

    public static class LinkData {
        public String href;
    }

    public static class ErrorManagement {
        public String errorCode;
        public String errorDescription;
        
    }

    public static class TppMessage {
        public String category; //ERROR-WARNING
        public String code;
        public String path;
        public String text;
       
    }

}