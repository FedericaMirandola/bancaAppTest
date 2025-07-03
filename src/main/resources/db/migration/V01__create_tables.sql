DROP TABLE IF EXISTS transaction CASCADE;
DROP TABLE IF EXISTS classification_rule CASCADE;
DROP TABLE IF EXISTS regola_classificazione CASCADE;

CREATE TABLE classification_rule (
    id BIGSERIAL PRIMARY KEY ,
    keyword VARCHAR(255) NOT NULL,
    center_type VARCHAR(255) NOT NULL
);

CREATE TABLE transaction (
     id BIGSERIAL PRIMARY KEY,
     account_id VARCHAR(255) NOT NULL,
     transaction_id VARCHAR(255) NOT NULL UNIQUE,
     booking_date TIMESTAMP WITH TIME ZONE,
     value_date TIMESTAMP WITH TIME ZONE,
     amount NUMERIC(19, 2) NOT NULL,
     currency VARCHAR(3) NOT NULL,
     remittance_information VARCHAR(2048),
     creditor_name VARCHAR(255),
     debtor_name VARCHAR(255),
     bank_transaction_code VARCHAR(255),
     proprietary_bank_transaction_code VARCHAR(255),
     additional_information VARCHAR(2048),
     center_type VARCHAR(20) NOT NULL CHECK (center_type IN ('COSTO', 'PROFITTO', 'UNDEFINED'))
        
);