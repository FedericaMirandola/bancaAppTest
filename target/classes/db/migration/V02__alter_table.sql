ALTER TABLE regola_classificazione
ADD COLUMN json_rule TEXT;

ALTER TABLE transaction
ADD CONSTRAINT uk_transaction_id UNIQUE (transaction_id);