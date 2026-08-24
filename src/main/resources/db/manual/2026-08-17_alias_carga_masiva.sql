-- Alias de columnas de instrumentos medicos, para la carga masiva de resultados.
--
-- Un alias es el nombre EXACTO con el que un aparato titula una columna en el
-- archivo que exporta. Son tablas hijas y no columnas porque un mismo parametro
-- puede recibir archivos de aparatos distintos que lo llaman de otra forma: el
-- tensiometro escribe "SYS" y la hoja del laboratorio escribe "Sistolica".
--
-- POR QUE SE REPITE AQUI EL ID DEL TIPO / DE LA INSTITUCION
--
-- La restriccion que importa es que dentro de un mismo tipo de estudio (o de una
-- misma institucion, en examenes) no haya dos alias iguales reclamando la misma
-- columna. Esa unicidad cruza dos tablas y SQL no sabe expresarla, asi que se
-- duplica el id del padre para poder imponerla con un indice unico.
--
-- No es una optimizacion: si dos alias iguales conviven, una columna del archivo
-- puede resolverse al parametro equivocado y el resultado clinico queda guardado
-- en el sitio incorrecto sin que nada avise.
--
-- LA UNICIDAD VA SOBRE alias_normalizado, NO SOBRE alias
--
-- alias_normalizado es el texto en mayusculas, sin acentos y con los espacios
-- colapsados. Si el indice fuera sobre el texto original, "Sistolica" y
-- "SISTOLICA" conviviran en la tabla y chocarian despues, al emparejar las
-- columnas del archivo, que es el peor momento para enterarse.
--
-- Hibernate (ddl-auto=update) crea ambas tablas al arrancar; este script queda
-- para los entornos donde el esquema se aplica a mano. Sin filas, el sistema se
-- comporta igual que antes: la carga masiva simplemente no reconoce ninguna
-- columna hasta que se configuren los alias.

CREATE TABLE IF NOT EXISTS alias_parametro_estudio (
    id_alias          BIGINT       NOT NULL AUTO_INCREMENT,
    id_parametro      BIGINT       NOT NULL,
    id_tipo_estudio   BIGINT       NOT NULL,
    alias             VARCHAR(150) NOT NULL,
    alias_normalizado VARCHAR(150) NOT NULL,
    orden             INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id_alias),
    CONSTRAINT uk_alias_tipo_estudio
        UNIQUE (id_tipo_estudio, alias_normalizado),
    CONSTRAINT fk_alias_parametro
        FOREIGN KEY (id_parametro) REFERENCES Parametro_Estudio (id_parametro)
        ON DELETE CASCADE,
    CONSTRAINT fk_alias_tipo_estudio
        FOREIGN KEY (id_tipo_estudio) REFERENCES Tipo_Estudio (id_tipo_estudio)
);

CREATE TABLE IF NOT EXISTS alias_examen (
    id_alias          BIGINT       NOT NULL AUTO_INCREMENT,
    id_examen         BIGINT       NOT NULL,
    id_institucion    BIGINT       NOT NULL,
    alias             VARCHAR(150) NOT NULL,
    alias_normalizado VARCHAR(150) NOT NULL,
    orden             INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id_alias),
    CONSTRAINT uk_alias_examen_institucion
        UNIQUE (id_institucion, alias_normalizado),
    CONSTRAINT fk_alias_examen
        FOREIGN KEY (id_examen) REFERENCES Examen (id_examen)
        ON DELETE CASCADE,
    CONSTRAINT fk_alias_examen_institucion
        FOREIGN KEY (id_institucion) REFERENCES Institucion (id_institucion)
);

-- El borrado en cascada desde el parametro / el examen es deliberado: un alias
-- sin su parametro no significa nada y dejarlo huerfano solo bloquearia el alta
-- de otro alias igual mas adelante.
