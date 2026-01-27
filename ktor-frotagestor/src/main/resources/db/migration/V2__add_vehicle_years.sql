-- V2__add_vehicle_years.sql
-- Adiciona anos específicos para veículos brasileiros (fabricação/modelo)
-- Preserva dados existentes: copia year → ambos os novos campos

ALTER TABLE vehicles
    ADD COLUMN manufacturing_year INTEGER NULL,
ADD COLUMN model_year INTEGER NULL;

-- Migra dados existentes (year → ambos campos para preservar histórico)
UPDATE vehicles
SET manufacturing_year = year,
    model_year = year
WHERE year IS NOT NULL;

-- Remove coluna obsoleta APÓS backup
-- ALTER TABLE vehicles DROP COLUMN year;  -- DESCOMENTE após validar

-- Índice para performance em filtros de ano
CREATE INDEX idx_vehicles_manufacturing_year ON vehicles(manufacturing_year);
CREATE INDEX idx_vehicles_model_year ON vehicles(model_year);
