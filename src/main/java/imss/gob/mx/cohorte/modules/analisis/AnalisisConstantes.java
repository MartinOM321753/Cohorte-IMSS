package imss.gob.mx.cohorte.modules.analisis;

/** Parámetros fijos del módulo de análisis (decisiones del diseño). */
public final class AnalisisConstantes {

    private AnalisisConstantes() {}

    /**
     * Umbral de supresión k-anónima: toda celda agregada con menos de este número
     * de participantes se oculta del resultado. Fijado en 5, global para todo el
     * proyecto (decisión de revisión R2).
     */
    public static final int K_ANONIMO = 5;

    /** Número de barras por defecto del histograma cuando el cliente no lo especifica. */
    public static final int BINS_DEFECTO = 12;

    /** Etiqueta de la dimensión cuando el valor del participante no está capturado. */
    public static final String SIN_DATO = "Sin dato";
}
