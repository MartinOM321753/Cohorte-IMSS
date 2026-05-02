CREATE TABLE Tipo_Estudio (
    id_tipo_estudio BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500) NULL,
    activo BIT(1) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    PRIMARY KEY (id_tipo_estudio)
);

CREATE TABLE Parametro_Estudio (
    id_parametro BIGINT NOT NULL AUTO_INCREMENT,
    id_tipo_estudio BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    unidad VARCHAR(20) NULL,
    PRIMARY KEY (id_parametro),
    CONSTRAINT fk_parametro_tipo_base FOREIGN KEY (id_tipo_estudio) REFERENCES Tipo_Estudio (id_tipo_estudio),
    CONSTRAINT uk_parametro_nombre_base UNIQUE (nombre)
);

CREATE TABLE Estudio_Medico (
    id_estudio BIGINT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id_estudio)
);

CREATE TABLE Resultado_Estudio (
    id_resultado BIGINT NOT NULL AUTO_INCREMENT,
    id_estudio BIGINT NOT NULL,
    id_parametro BIGINT NOT NULL,
    valor_numerico DOUBLE NULL,
    valor_texto VARCHAR(255) NULL,
    PRIMARY KEY (id_resultado),
    CONSTRAINT fk_resultado_estudio_base FOREIGN KEY (id_estudio) REFERENCES Estudio_Medico (id_estudio),
    CONSTRAINT fk_resultado_parametro_base FOREIGN KEY (id_parametro) REFERENCES Parametro_Estudio (id_parametro),
    CONSTRAINT uk_estudio_parametro UNIQUE (id_estudio, id_parametro)
);
