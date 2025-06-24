DROP TABLE IF EXISTS transaction CASCADE;
DROP TABLE IF EXISTS regola_classificazione CASCADE;
DROP TABLE IF EXISTS center CASCADE;

CREATE TABLE regola_classificazione (
    id BIGSERIAL PRIMARY KEY,
    parola_chiave TEXT NOT NULL,
    center VARCHAR(20) NOT NULL CHECK (center IN ('COSTO', 'PROFITTO'))
);

CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_id TEXT,
    booking_date TIMESTAMPTZ,
    value_date TIMESTAMPTZ,
    currency VARCHAR(10),
    amount NUMERIC,
    remittance_information TEXT,
    creditor_name TEXT,
    debtor_name TEXT,
    bank_transaction_code TEXT,
    additional_information TEXT,
    proprietary_bank_transaction_code TEXT,
    account_id TEXT,
    center VARCHAR(20) NOT NULL CHECK (center IN ('COSTO', 'PROFITTO'))
);