ALTER TABLE transaction DROP CONSTRAINT IF EXISTS transaction_currency_check;
ALTER TABLE transaction ALTER COLUMN currency TYPE VARCHAR(3);
