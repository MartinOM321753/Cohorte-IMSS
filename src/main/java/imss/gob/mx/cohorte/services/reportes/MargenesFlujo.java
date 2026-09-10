package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * La separación entre el contenido y el borde del papel en una hoja de flujo.
 *
 * <p>Sólo aplica a las hojas de flujo. En el lienzo cada elemento lleva sus
 * milímetros medidos desde la esquina del papel, así que un margen de página movería
 * todos los diseños que ya existen.</p>
 *
 * @param arribaMm     separación superior, la misma en todas las páginas
 * @param derechaMm    separación derecha
 * @param abajoMm      separación inferior
 * @param izquierdaMm  separación izquierda
 */
public record MargenesFlujo(double arribaMm, double derechaMm,
                            double abajoMm, double izquierdaMm) {

    /** Lo que usa un diseño que no dice nada: los márgenes de una carta corriente. */
    public static final MargenesFlujo POR_DEFECTO = new MargenesFlujo(18, 16, 18, 16);

    /** Tope: por encima de esto no queda papel útil en un A4. */
    private static final double MAXIMO_MM = 60;

    /** Los del diseño, cayendo a los de siempre donde falten o no tengan sentido. */
    public static MargenesFlujo de(JsonNode diseno) {
        JsonNode m = diseno.path("margenesFlujo");
        if (m.isMissingNode() || !m.isObject()) return POR_DEFECTO;
        return new MargenesFlujo(
                medida(m, "arribaMm", POR_DEFECTO.arribaMm()),
                medida(m, "derechaMm", POR_DEFECTO.derechaMm()),
                medida(m, "abajoMm", POR_DEFECTO.abajoMm()),
                medida(m, "izquierdaMm", POR_DEFECTO.izquierdaMm()));
    }

    /**
     * Un margen negativo o descomunal no se corrige en silencio a medias: se descarta
     * y se usa el de siempre. Un documento clínico con el contenido fuera del papel
     * es peor que uno con márgenes que nadie eligió.
     */
    private static double medida(JsonNode m, String campo, double porDefecto) {
        double v = m.path(campo).asDouble(porDefecto);
        return v >= 0 && v <= MAXIMO_MM ? v : porDefecto;
    }
}
