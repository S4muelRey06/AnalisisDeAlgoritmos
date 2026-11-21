ALTER TABLE estudiante
    ADD COLUMN IF NOT EXISTS horario_confirmado text,
    ADD COLUMN IF NOT EXISTS promedio_confirmado double precision;
