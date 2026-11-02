package imss.gob.mx.cohorte.modules.analisis;

/**
 * Cómo se agrega una {@link Metrica}.
 *
 * <p>El tipo manda en la agregación: lo {@code NUMERICO} admite media, mediana,
 * percentiles, histograma y % fuera de rango; lo {@code CATEGORICO} solo
 * frecuencias y proporciones. Los tipos de parámetro del dominio
 * (NUMERICO / TEXTO / BOOLEANO / TEXTO_OPCIONES) colapsan aquí a estos dos.</p>
 */
public enum TipoMetrica {
    NUMERICO,
    CATEGORICO
}
