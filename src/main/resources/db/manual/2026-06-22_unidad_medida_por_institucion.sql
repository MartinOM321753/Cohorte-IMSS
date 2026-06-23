-- Migracion manual para convertir Unidad_Medida de catalogo global a catalogo por institucion.
-- Ejecutar una sola vez en bases existentes antes de levantar la app con el nuevo modelo.
-- MySQL.

START TRANSACTION;

ALTER TABLE Unidad_Medida
    ADD COLUMN id_institucion BIGINT NULL;

UPDATE Unidad_Medida
SET id_institucion = (SELECT i.id_institucion FROM Institucion i ORDER BY i.id_institucion LIMIT 1)
WHERE id_institucion IS NULL;

ALTER TABLE Unidad_Medida
    DROP INDEX uk_unidad_nombre;

INSERT INTO Unidad_Medida (nombre, activo, id_institucion)
SELECT base.nombre, base.activo, i.id_institucion
FROM (
    SELECT nombre, activo, MIN(id_institucion) AS id_institucion_base
    FROM Unidad_Medida
    GROUP BY nombre, activo
) base
JOIN Institucion i ON i.id_institucion <> base.id_institucion_base
WHERE NOT EXISTS (
    SELECT 1
    FROM Unidad_Medida existente
    WHERE LOWER(existente.nombre) = LOWER(base.nombre)
      AND existente.id_institucion = i.id_institucion
);

ALTER TABLE Unidad_Medida
    MODIFY id_institucion BIGINT NOT NULL;

ALTER TABLE Unidad_Medida
    ADD CONSTRAINT uk_unidad_nombre_inst UNIQUE (nombre, id_institucion);

ALTER TABLE Unidad_Medida
    ADD CONSTRAINT fk_unidad_medida_institucion
    FOREIGN KEY (id_institucion) REFERENCES Institucion (id_institucion);

COMMIT;
