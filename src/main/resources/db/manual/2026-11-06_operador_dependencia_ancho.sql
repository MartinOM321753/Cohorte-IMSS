-- Ancho de operador_dependencia, por los operadores SIN_RESPUESTA y CON_RESPUESTA.
--
-- Motivo: la columna era VARCHAR(10) y los dos operadores nuevos miden 13
-- caracteres. Sin este ALTER, guardar una pregunta con SIN_RESPUESTA falla (MySQL
-- en modo estricto) o se trunca a «SIN_RESPUE» (modo permisivo), y la lectura
-- posterior del enum revienta.
--
-- Hibernate (ddl-auto=update) NO ensancha una columna que ya existe, aunque la
-- entidad diga VARCHAR(20); por eso hay que correr esto a mano en las bases con datos.
--
-- Hay que correrlo ANTES de cargar un diccionario que use los operadores nuevos
-- (el del Cuestionario General trae el salto de la P191).
--
-- Es seguro correrlo antes o después del despliegue: solo ensancha, no toca datos.
-- Conserva el DEFAULT 'IGUAL', que es el que evita que las preguntas existentes
-- nazcan con DISTINTO (ver VariableCuestionario#operadorDependencia).

ALTER TABLE variable_cuestionario
    MODIFY COLUMN operador_dependencia VARCHAR(20) NOT NULL DEFAULT 'IGUAL';

-- Comprobacion (debe devolver 20). Si devuelve 10, el ALTER no corrio en ESTA base.
-- La carga del catalogo hace la misma medicion y se niega a cargar un archivo con
-- SIN_RESPUESTA / CON_RESPUESTA mientras la columna siga angosta
-- (CargaCatalogoService#exigirColumnaDeOperadorAncha).
SELECT CHARACTER_MAXIMUM_LENGTH AS ancho_operador_dependencia
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'variable_cuestionario'
   AND COLUMN_NAME = 'operador_dependencia';

-- Por si ya se cargo antes de ensanchar (modo permisivo de MySQL): filas truncadas.
-- Debe devolver 0 filas; si devuelve alguna, hay que volver a cargar el catalogo.
SELECT codigo, operador_dependencia
  FROM variable_cuestionario
 WHERE operador_dependencia NOT IN ('IGUAL', 'DISTINTO', 'EN', 'SIN_RESPUESTA', 'CON_RESPUESTA');
