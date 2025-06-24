ALTER TABLE center ADD COLUMN is_default BOOLEAN DEFAULT FALSE;

-- Imposta un center come default (solo se non già presente)
-- Nota: va fatto **dopo** aver aggiunto la colonna
INSERT INTO center (nome, tipo, is_default)
SELECT 'Centro di default', 'COSTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM center WHERE is_default = TRUE
);