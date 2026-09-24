package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import java.util.List;

/**
 * Resultado de planificar un lote: cuántas alícuotas caben en lo que realmente
 * se extrajo, qué sobra y qué se le puede ofrecer al usuario.
 *
 * <p>Lo consume tanto la previsualización del formulario como la creación del
 * lote, así que lo que la pantalla promete y lo que el servidor crea salen del
 * mismo cálculo.</p>
 *
 * @param numeroAlicuotasConfiguradas tope del tubo; nunca se generan más
 * @param volumenAlicuota             capacidad de cada alícuota
 * @param unidad                      unidad de todo el lote y de la muestra padre
 * @param totalRequerido              lo que haría falta para el lote completo
 * @param valorDisponible             lo que la muestra padre puede comprometer
 * @param alicuotasCompletas          cuántas salen llenas
 * @param remanente                   lo que queda tras llenar las completas
 * @param lugaresRestantes            huecos del tubo que quedarían sin usar
 * @param volumenesSugeridos          el plan por omisión: solo las completas
 * @param puedeAlojarParcial          si el remanente cabe en una alícuota incompleta
 * @param alcanzaLoteCompleto         si el volumen da para todas las que faltan
 * @param slotsOcupados               alícuotas del lote que ya existen
 * @param slotsLibres                 huecos del tubo que quedan por llenar
 * @param mensaje                     explicación redactada para el usuario
 */
public record PlanAlicuotas(
        int numeroAlicuotasConfiguradas,
        double volumenAlicuota,
        String unidad,
        double totalRequerido,
        double valorDisponible,
        int alicuotasCompletas,
        double remanente,
        int lugaresRestantes,
        List<Double> volumenesSugeridos,
        boolean puedeAlojarParcial,
        boolean alcanzaLoteCompleto,
        int slotsOcupados,
        int slotsLibres,
        String mensaje
) {
    /** Si el lote ya venía empezado y esto es una continuación. */
    public boolean esContinuacion() {
        return slotsOcupados > 0;
    }

    /** Cuántas alícuotas propone crear el plan por omisión. */
    public int totalSugerido() {
        return volumenesSugeridos.size();
    }
}
