-- Distingue las dos formas que puede tener un traslado en EN_DEVOLUCION, y repara
-- las devoluciones de alicuotas que quedaron bloqueadas por confundirlas.
--
-- El problema: una fila de prestamo de ida reutiliza su propio registro para
-- representar la vuelta, asi que ahi `id_institucion_destino` sigue siendo QUIEN
-- TIENE la muestra. En cambio, las filas que crea la propia devolucion para las
-- alicuotas que viajan con su padre describen el trayecto tal cual: el tenedor va
-- en `id_institucion_origen` y el destino final en `id_institucion_destino`.
--
-- Confirmar la devolucion leia ambas con la misma regla. Con la fila de una
-- alicuota comparaba al tenedor contra el destino final y concluia que la muestra
-- "ya no se encontraba" donde debia, dejandola imposible de confirmar. Y de haber
-- pasado esa comprobacion, habria mandado la alicuota a la institucion donde ya
-- estaba, separandola de su padre en silencio.
--
-- Se resuelve marcando explicitamente la forma de cada fila. Hibernate agrega la
-- columna sola al arrancar (ddl-auto=update) con el DEFAULT del columnDefinition,
-- que deja en 0 todo lo existente: exactamente lo correcto para los prestamos de
-- ida, que son la inmensa mayoria. Este script queda para los entornos donde el
-- esquema se aplica a mano, y ademas repara los datos, que Hibernate no toca.

ALTER TABLE Traslado_Muestra
    ADD COLUMN IF NOT EXISTS es_movimiento_devolucion BIT(1) NOT NULL DEFAULT b'0';

-- ── Reparacion de datos ─────────────────────────────────────────────────────
--
-- Una fila creada por la devolucion se reconoce sin ambiguedad: esta en
-- EN_DEVOLUCION, pertenece a un grupo de devolucion, y su muestra NO esta en la
-- institucion que la fila declara como destino sino en la que declara como
-- origen. En un prestamo de ida es justo al reves, asi que la condicion separa
-- las dos formas sin tocar las que ya estaban bien.
--
-- Antes de ejecutar el UPDATE conviene revisar que filas va a alcanzar:

SELECT t.id_traslado,
       m.etiqueta,
       t.estado,
       t.id_institucion_origen  AS origen,
       t.id_institucion_destino AS destino,
       m.id_institucion_actual  AS tenedor_real,
       t.grupo_devolucion
  FROM Traslado_Muestra t
  JOIN Muestra m ON m.id_muestra = t.id_muestra
 WHERE t.estado = 'EN_DEVOLUCION'
   AND t.grupo_devolucion IS NOT NULL
   AND m.id_institucion_actual = t.id_institucion_origen
   AND m.id_institucion_actual <> t.id_institucion_destino;

UPDATE Traslado_Muestra t
  JOIN Muestra m ON m.id_muestra = t.id_muestra
   SET t.es_movimiento_devolucion = b'1'
 WHERE t.estado = 'EN_DEVOLUCION'
   AND t.grupo_devolucion IS NOT NULL
   AND m.id_institucion_actual = t.id_institucion_origen
   AND m.id_institucion_actual <> t.id_institucion_destino;

-- Comprobacion: no debe quedar ninguna fila en EN_DEVOLUCION cuyo tenedor real no
-- coincida con lo que la fila espera segun su forma. Si esta consulta devuelve
-- algo, esa devolucion seguira sin poder confirmarse y hay que mirarla a mano.

SELECT t.id_traslado,
       m.etiqueta,
       t.es_movimiento_devolucion,
       t.id_institucion_origen  AS origen,
       t.id_institucion_destino AS destino,
       m.id_institucion_actual  AS tenedor_real
  FROM Traslado_Muestra t
  JOIN Muestra m ON m.id_muestra = t.id_muestra
 WHERE t.estado = 'EN_DEVOLUCION'
   AND m.id_institucion_actual <> IF(t.es_movimiento_devolucion,
                                     t.id_institucion_origen,
                                     t.id_institucion_destino);
