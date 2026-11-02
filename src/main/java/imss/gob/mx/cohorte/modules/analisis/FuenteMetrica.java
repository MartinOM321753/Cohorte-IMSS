package imss.gob.mx.cohorte.modules.analisis;

/**
 * De cuál de los cuatro circuitos de datos proviene una {@link Metrica}.
 *
 * <p>Las cuatro fuentes guardan sus resultados de forma distinta —dos como
 * clave-valor tipado ({@code ESTUDIO_MEDICO}, {@code ESTUDIO_MUESTRA}), una como
 * columna numérica con rango ({@code EXAMEN}) y otra como columnas fijas
 * ({@code SOMATOMETRIA})—; la capa de análisis las unifica bajo el concepto de
 * métrica para poder agregarlas y compararlas con la misma maquinaria.</p>
 */
public enum FuenteMetrica {
    ESTUDIO_MEDICO,
    ESTUDIO_MUESTRA,
    EXAMEN,
    SOMATOMETRIA
}
