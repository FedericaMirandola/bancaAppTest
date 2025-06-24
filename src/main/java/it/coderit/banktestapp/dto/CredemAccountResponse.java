package it.coderit.banktestapp.dto;

import java.util.List;


//  DTO (Data Transfer Object) per la risposta dell'API Credem che elenca gli account.

public class CredemAccountResponse {
    // La lista di oggetti AccountData, che rappresentano i singoli account disponibili.
    // Il nome del campo 'accounts' deve corrispondere esattamente alla chiave JSON.
    public List<AccountData> accounts;


    public static class AccountData {
        
        public String resourceId;
        
        public String iban;
        
        public String currency;
        
        public String product;
        
    }
}