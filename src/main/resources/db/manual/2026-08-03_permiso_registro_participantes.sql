-- Autorización para que una institución hija registre participantes a nombre de
-- otras instituciones de su mismo grupo (el padre y las hermanas).
--
-- Es una tabla aparte de permiso_acceso_pacientes a propósito: esa decide quién
-- PUEDE VER los pacientes de otra sede, y esta quién puede DARLOS DE ALTA a
-- nombre de otra. Activar una no debe conceder la otra.
--
-- Hibernate (ddl-auto=update) crea la tabla al arrancar; este script queda para
-- los entornos donde el esquema se aplica a mano. Sin filas, el comportamiento
-- es el de siempre: cada sede solo registra para sí misma y para sus
-- descendientes.

CREATE TABLE IF NOT EXISTS permiso_registro_participantes (
    id_permiso_registro    BIGINT       NOT NULL AUTO_INCREMENT,
    id_institucion_otorga  BIGINT       NOT NULL,
    id_institucion_recibe  BIGINT       NOT NULL,
    habilitado             BIT(1)       NOT NULL DEFAULT b'1',
    fecha_otorgamiento     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id_permiso_registro),
    CONSTRAINT uk_permiso_registro_otorga_recibe
        UNIQUE (id_institucion_otorga, id_institucion_recibe),
    CONSTRAINT fk_permiso_registro_otorga
        FOREIGN KEY (id_institucion_otorga) REFERENCES Institucion (id_institucion),
    CONSTRAINT fk_permiso_registro_recibe
        FOREIGN KEY (id_institucion_recibe) REFERENCES Institucion (id_institucion)
);
