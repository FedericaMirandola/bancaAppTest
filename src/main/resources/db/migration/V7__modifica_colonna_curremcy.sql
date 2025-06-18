ALTER TABLE movimento
    ALTER COLUMN currency TYPE VARCHAR(10),
    DROP CONSTRAINT IF EXISTS movimento_currency_check;
