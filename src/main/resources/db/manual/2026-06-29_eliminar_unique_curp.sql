-- Elimina la restricción UNIQUE de la columna curp en la tabla persona.
-- Motivo: la carga masiva de pacientes ahora tolera CURP duplicado o
-- incompleto (pseudoCURP); la unicidad se valida solo por código en el
-- flujo de registro manual (PersonaService). Ejecutar manualmente en cada
-- entorno (Flyway está deshabilitado, Hibernate ddl-auto=update no elimina
-- constraints existentes).

-- 1. Identificar el nombre real del índice único (Hibernate lo autogenera,
--    no se llama "curp").
SHOW INDEX FROM persona WHERE Column_name = 'curp' AND Non_unique = 0;

-- 2. Eliminarlo usando el nombre obtenido en el paso anterior.
--    Reemplazar <Key_name_obtenido> por el valor real devuelto arriba.
-- ALTER TABLE persona DROP INDEX <Key_name_obtenido>;
