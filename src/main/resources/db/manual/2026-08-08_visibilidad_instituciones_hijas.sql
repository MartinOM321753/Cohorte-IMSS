-- Una institución decide de qué hijas quiere ver los participantes.
--
-- Hasta ahora «las hijas siempre se ven» era una constante escrita dentro del
-- recorrido del árbol. Esto la convierte en una decisión, pero guardando solo
-- las excepciones: si no hay fila para una hija, manda el valor por defecto de
-- la institución padre (Institucion.ver_participantes_hijas).
--
-- La columna arranca en 1 para que el sistema se comporte exactamente igual que
-- antes hasta que alguien decida lo contrario.
--
-- Lo que aquí se apaga es la vista de PARTICIPANTES, no la administración: el
-- padre sigue gestionando usuarios, catálogos y módulos de esa hija, y la sigue
-- viendo en la pantalla de instituciones. Si no fuera así, ocultar una hija
-- sería irreversible.
--
-- Hibernate (ddl-auto=update) aplica ambos cambios al arrancar; este script
-- queda para los entornos donde el esquema se aplica a mano.

ALTER TABLE Institucion
    ADD COLUMN IF NOT EXISTS ver_participantes_hijas BIT(1) NOT NULL DEFAULT b'1';

CREATE TABLE IF NOT EXISTS visibilidad_institucion_hija (
    id_visibilidad         BIGINT       NOT NULL AUTO_INCREMENT,
    id_institucion_padre   BIGINT       NOT NULL,
    id_institucion_hija    BIGINT       NOT NULL,
    ver_participantes      BIT(1)       NOT NULL DEFAULT b'1',
    fecha_actualizacion    DATETIME(6)  NOT NULL,
    usuario_uuid           VARCHAR(36)  NULL,
    PRIMARY KEY (id_visibilidad),
    CONSTRAINT uk_visibilidad_padre_hija
        UNIQUE (id_institucion_padre, id_institucion_hija),
    CONSTRAINT fk_visibilidad_padre
        FOREIGN KEY (id_institucion_padre) REFERENCES Institucion (id_institucion),
    CONSTRAINT fk_visibilidad_hija
        FOREIGN KEY (id_institucion_hija) REFERENCES Institucion (id_institucion)
);
