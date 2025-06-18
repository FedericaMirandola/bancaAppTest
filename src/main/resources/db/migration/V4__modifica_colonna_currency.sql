ALTER TABLE movimento DROP CONSTRAINT IF EXISTS movimento_currency_check;
ALTER TABLE movimento ALTER COLUMN currency TYPE VARCHAR(3);
