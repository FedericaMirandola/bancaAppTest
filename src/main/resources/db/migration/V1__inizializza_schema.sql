CREATE TABLE center (
    id BIGSERIAL PRIMARY KEY,
    nome TEXT NOT NULL,
    tipo TEXT NOT NULL 
);

CREATE TABLE regola_classificazione (
    id BIGSERIAL PRIMARY KEY,
    parola_chiave TEXT NOT NULL,
    center_id BIGINT NOT NULL REFERENCES center(id)
);

CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_id TEXT,
    booking_date TIMESTAMPTZ,
    value_date TIMESTAMPTZ,
    currency TEXT,
    amount NUMERIC,
    remittance_information TEXT,
    creditor_name TEXT,
    debtor_name TEXT,
    bank_transaction_code TEXT,
    additional_information TEXT,
    proprietary_bank_transaction_code TEXT,
    account_id TEXT,
    center_id BIGINT NOT NULL REFERENCES center(id)
);
