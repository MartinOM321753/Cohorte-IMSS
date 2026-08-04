-- Agrega el ancho de la barra angosta a la configuración de etiqueta.
-- Motivo: en Code 128 el ancho de barra lo fija ^BY, que el generador de ZPL
-- nunca emitía, así que quedaba clavado en el valor por omisión de la impresora
-- (2 dots) y no había forma de ensanchar el código. El campo solo aplica a los
-- códigos lineales; en DataMatrix y QR el tamaño sigue en modulo_codigo.
--
-- Hibernate (ddl-auto=update) agrega esta columna solo al arrancar, y el DEFAULT
-- del columnDefinition ya rellena las filas existentes con 2 — que es justo el
-- valor con el que se venían imprimiendo, así que ninguna etiqueta cambia de
-- tamaño al actualizar. Este script queda para los entornos donde el esquema se
-- aplica a mano.

ALTER TABLE configuracion_etiqueta
    ADD COLUMN ancho_barra_codigo INT NOT NULL DEFAULT 2;
