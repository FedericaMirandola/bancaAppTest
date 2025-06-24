-- V4__unique_transaction_id.sql
ALTER TABLE transaction
ADD CONSTRAINT uk_transaction_id UNIQUE (transaction_id);
-- Questa constraint garantisce che ogni transaction_id sia unico nella tabella transaction.
-- Se esiste già un transaction_id duplicato, la query fallirà.