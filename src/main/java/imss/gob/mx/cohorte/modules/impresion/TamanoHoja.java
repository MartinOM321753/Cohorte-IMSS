package imss.gob.mx.cohorte.modules.impresion;

/**
 * Tamaño de la hoja sobre la que se acomodan las etiquetas Avery.
 *
 * Las medidas viajan al navegador para emitir {@code @page { size: ... }} con
 * valores explícitos en milímetros. Dejarlo en {@code size: letter} obligaba a
 * que el tamaño estuviera repetido como constante en el frontend, y una hoja A4
 * quedaba mal acomodada sin que nada lo advirtiera.
 */
public enum TamanoHoja {

    CARTA(215.9, 279.4),
    A4(210.0, 297.0);

    private final double anchoMm;
    private final double altoMm;

    TamanoHoja(double anchoMm, double altoMm) {
        this.anchoMm = anchoMm;
        this.altoMm = altoMm;
    }

    public double getAnchoMm() {
        return anchoMm;
    }

    public double getAltoMm() {
        return altoMm;
    }
}
