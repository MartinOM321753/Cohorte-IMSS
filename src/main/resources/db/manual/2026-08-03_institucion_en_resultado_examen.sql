-- Institución propia para los resultados de examen.
--
-- Era la única entidad clínica que no la tenía: heredaba la del paciente. Eso
-- causaba dos problemas — al mover un participante de sede sus resultados
-- cambiaban de dueño en silencio, y una institución no podía alcanzar los
-- resultados que ella misma capturó a un participante que ya no gestiona.
--
-- ────────────────────────────────────────────────────────────────────────────
-- DESPLIEGUE CON DATOS EXISTENTES
--
-- Hibernate (ddl-auto=update) agrega la columna al arrancar. El DEFAULT 1 del
-- columnDefinition permite hacerlo sobre una tabla con filas sin que falle.
--
-- En un entorno con UNA SOLA institución eso ya deja el valor correcto y no hay
-- nada más que hacer: todos los resultados son de esa institución.
--
-- En un entorno con VARIAS instituciones el DEFAULT 1 sería incorrecto para los
-- registros de las demás sedes. El UPDATE de abajo lo corrige derivándolo del
-- participante, que es lo que el sistema asumía implícitamente hasta ahora.
-- ────────────────────────────────────────────────────────────────────────────

-- 1. Si el esquema se aplica a mano (sin Hibernate), crear la columna:
-- ALTER TABLE resultado_examen
--     ADD COLUMN id_institucion BIGINT NOT NULL DEFAULT 1,
--     ADD CONSTRAINT fk_resultado_examen_institucion
--         FOREIGN KEY (id_institucion) REFERENCES Institucion (id_institucion);

-- 2. Corregir los registros existentes tomando la institución del participante.
--
--    ⚠ EJECUTAR UNA SOLA VEZ, y solo en entornos con varias instituciones.
--    Reejecutarlo más tarde sobreescribiría los resultados que una sede capturó
--    legítimamente a un participante de otra, forzándolos a la institución del
--    participante. Por eso va acotado por fecha: sustituir la fecha de abajo por
--    el momento del despliegue, de modo que solo alcance a lo anterior.
--
--    En un entorno de una sola institución este paso es innecesario: el DEFAULT 1
--    ya dejó el valor correcto.

-- UPDATE resultado_examen r
--     JOIN Paciente p ON p.id_paciente = r.id_paciente
-- SET r.id_institucion = p.id_institucion
-- WHERE r.fecha_registro < '2026-08-03 00:00:00';

-- 3. Comprobación: cuántos resultados quedaron por institución.
SELECT id_institucion, COUNT(*) AS resultados
FROM resultado_examen
GROUP BY id_institucion;
