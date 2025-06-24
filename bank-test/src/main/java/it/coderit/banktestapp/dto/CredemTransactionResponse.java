package it.coderit.banktestapp.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.coderit.banktestapp.model.Currency;
import lombok.Data;

@Data
public class CredemTransactionResponse {

    @JsonProperty("booked")
    public List<TransactionData> booked;

    public static class TransactionData {
        public String transactionId;
        public String bookingDate;
        public String valueDate; 
        public Amount transactionAmount;
        public String remittanceInformationUnstructured;
        public String creditorName;
        public String debtorName;
        public String bankTransactionCode;
        public String proprietaryBankTransactionCode;
        public String additionalInformation;
        public String accountId;

        public static class Amount {
            public Currency currency;
            public BigDecimal amount;
        }
    }
}
