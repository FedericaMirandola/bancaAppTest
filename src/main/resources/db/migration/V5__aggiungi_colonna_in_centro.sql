ALTER TABLE centro ADD COLUMN is_default BOOLEAN DEFAULT FALSE;

-- Imposta un centro come default (solo se non già presente)
-- Nota: va fatto **dopo** aver aggiunto la colonna
INSERT INTO centro (nome, tipo, is_default)
SELECT 'Centro di default', 'COSTO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM centro WHERE is_default = TRUE
);