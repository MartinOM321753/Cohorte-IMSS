package imss.gob.mx.cohorte.modules.impresion;

/**
 * Soporte físico sobre el que se imprime.
 *
 * Antes no existía: una misma configuración servía a la hoja Avery y al rollo
 * Zebra, y campos como {@code etiquetasPorFila} tenían que significar dos cosas
 * a la vez —columnas de la hoja y carriles del rollo—, que rara vez coinciden.
 */
public enum TipoMedio {

    /** Hoja suelta de etiquetas troqueladas, impresa por el navegador. */
    HOJA_AVERY,

    /** Rollo continuo o troquelado, impreso enviando ZPL a la Zebra. */
    ROLLO_ZEBRA
}
