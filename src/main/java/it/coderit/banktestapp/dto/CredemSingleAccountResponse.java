package it.coderit.banktestapp.dto;

import it.coderit.banktestapp.dto.CredemAccountResponse.AccountData;
import it.coderit.banktestapp.dto.CredemAccountResponse.ErrorManagement;
import it.coderit.banktestapp.dto.CredemAccountResponse.TppMessage;

public class CredemSingleAccountResponse {

    // L'oggetto AccountData rappresenta un singolo account con i suoi dettagli.
    public AccountData account;

    public ErrorManagement errorManagement;
    public TppMessage tppMessages;
}