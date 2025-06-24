package it.coderit.banktestapp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;


import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    public Long id;

    @Column(name = "transaction_id", length = 255)
    public String transactionId;

    @Column(name = "booking_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")  // ISO offset datetime per fuso orario
    public OffsetDateTime bookingDate;

    @Column(name= "value_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    public OffsetDateTime valueDate;

    @Column(length = 3)
    public String currency;

    @Column(precision = 38, scale = 2)
    public BigDecimal amount;

    @Column(length = 255, name = "remittance_information")
    public String remittanceInformation;

    @Column(length = 255, name = "creditor_name")
    public String creditorName;

    @Column(length = 255, name = "debtor_name")
    public String debtorName;

    @Column(length = 255, name = "bank_transaction_code")
    public String bankTransactionCode;

    @Column(length = 255, name = "additional_information")
    public String additionalInformation;

    @Column(length = 255, name = "proprietary_bank_transaction_code")
    public String proprietaryBankTransactionCode;

    @Column(length = 255, name = "account_id")
    public String accountId;

    @Column(name = "center", nullable = false)
    @Enumerated(EnumType.STRING)
    public CenterType centerType; 
}
