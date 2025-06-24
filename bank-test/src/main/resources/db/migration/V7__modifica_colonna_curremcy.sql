ALTER TABLE transaction
    ALTER COLUMN currency TYPE VARCHAR(10),
    DROP CONSTRAINT IF EXISTS transaction_currency_check;
