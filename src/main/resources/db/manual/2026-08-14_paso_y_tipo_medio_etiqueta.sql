-- Acomodo de etiquetas por paso, y separación del soporte físico.
--
-- Motivo: la posición de cada fila se venía calculando acumulando alto +
-- separación fila tras fila. Cualquier diferencia entre esa suma y el paso real
-- de la hoja se multiplicaba por el número de fila, así que la primera fila
-- salía bien y la décima caía varios milímetros fuera de su recuadro. El paso
-- es el dato que traen los catálogos de hoja y el único que posiciona bien una
-- cuadrícula, de modo que pasa a ser la fuente de verdad.
--
-- Motivo del tipo de medio: una sola configuración servía a la hoja Avery y al
-- rollo Zebra, y etiquetas_por_fila significaba a la vez columnas de la hoja y
-- carriles del rollo. Como la Zebra avanza el papel por filas completas, una
-- configuración de hoja con 3 columnas obligaba al rollo a consumir 3 etiquetas
-- por avance aunque solo se hubiera mandado una.
--
-- Hibernate (ddl-auto=update) agrega estas columnas al arrancar con el DEFAULT
-- del columnDefinition. Los UPDATE de abajo son los que conservan el
-- comportamiento actual: sin ellos las columnas quedan en cero, y los accesores
-- efectivos de la entidad caen al cálculo antiguo, que es exactamente lo que se
-- venía imprimiendo. Es decir, este script se puede correr antes o después del
-- despliegue sin que ninguna etiqueta cambie de tamaño ni de posición.

ALTER TABLE configuracion_etiqueta
    ADD COLUMN tipo_medio VARCHAR(20) NOT NULL DEFAULT 'HOJA_AVERY',
    ADD COLUMN tamano_hoja VARCHAR(10) NOT NULL DEFAULT 'CARTA',
    ADD COLUMN paso_horizontal_mm DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN paso_vertical_mm DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN margen_derecho_mm DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN margen_inferior_mm DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN ajuste_x_mm DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN ajuste_y_mm DOUBLE NOT NULL DEFAULT 0,
    ADD COLUMN carriles_rollo INT NOT NULL DEFAULT 0,
    ADD COLUMN ancho_cabezal_mm DOUBLE NOT NULL DEFAULT 104.0,
    ADD COLUMN offset_lh_x_dots INT NOT NULL DEFAULT 0,
    ADD COLUMN offset_lh_y_dots INT NOT NULL DEFAULT 0;

-- El paso que estaba vigente de hecho, aunque nadie lo hubiera capturado como
-- tal. Deja las hojas imprimiendo igual que hoy; corregir el desfase requiere
-- medir el paso real de la hoja y capturarlo, para lo que está la calibración.
UPDATE configuracion_etiqueta
   SET paso_horizontal_mm = ancho_mm + espacio_horizontal_mm,
       paso_vertical_mm   = alto_mm + espacio_vertical_mm
 WHERE paso_horizontal_mm = 0
    OR paso_vertical_mm = 0;

-- El área útil se calculaba como ancho - 2 * margen_izquierdo, o sea simétrica.
UPDATE configuracion_etiqueta
   SET margen_derecho_mm = margen_izquierdo_mm
 WHERE margen_derecho_mm = 0;

-- Los carriles del rollo son los que el generador de ZPL venía suponiendo.
UPDATE configuracion_etiqueta
   SET carriles_rollo = etiquetas_por_fila
 WHERE carriles_rollo = 0;
