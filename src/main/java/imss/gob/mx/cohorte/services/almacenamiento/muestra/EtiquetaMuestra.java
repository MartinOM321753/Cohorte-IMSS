package imss.gob.mx.cohorte.services.almacenamiento.muestra;

/**
 * Cómo se escribe la etiqueta de una muestra y de sus alícuotas.
 *
 * <p>La etiqueta no es un dato que se capture: se calcula. Vive aquí y no dentro
 * del alta de muestras porque tiene dos clientes —el registro de una muestra y
 * la carga masiva— y el formato tiene que ser exactamente el mismo en los dos.
 * Si cada uno lo armara por su cuenta, un vial cargado en bloque y otro
 * registrado a mano llevarían rótulos distintos para el mismo sitio, y la
 * etiqueta es lo único que hay pegado al tubo físico.</p>
 *
 * <p>El prefijo sale del tubo. Cuando el tubo no tiene ninguno configurado se
 * usa {@value #PREFIJO_POR_OMISION}, que es lo que hacía el alta desde que
 * existe: cambiarlo ahora renombraría las muestras que ya están rotuladas.</p>
 */
public final class EtiquetaMuestra {

    /** Lo que se usa cuando el tubo no define prefijo. */
    public static final String PREFIJO_POR_OMISION = "M";

    private EtiquetaMuestra() {}

    /** El prefijo del tubo, o el de omisión si no tiene. */
    public static String prefijo(String prefijoTubo) {
        return prefijoTubo != null && !prefijoTubo.isBlank() ? prefijoTubo : PREFIJO_POR_OMISION;
    }

    /**
     * Etiqueta de una muestra padre. Ej. {@code S/001103/I1F4-L2}.
     *
     * <p>Formato heredado (basado en número de lote). Lo sigue usando la carga
     * masiva hasta que se adapte a protocolos; el procesamiento por protocolo usa
     * {@link #primario}.</p>
     *
     * @param lote número de lote de esta muestra para ese folio y prefijo
     */
    public static String padre(String prefijoTubo, String folio, Long idInstitucion, int lote) {
        return prefijo(prefijoTubo) + "/" + folio + "/I" + idInstitucion + "F4-L" + lote;
    }

    /**
     * Etiqueta de un tubo primario de un protocolo. Ej. {@code SL/001104/I1F4-T1}.
     *
     * <p>El tubo primario se identifica por su <b>orden</b> dentro del protocolo
     * (T1…TN), no por un número de lote: el lote es de las alícuotas, no del tubo.
     * Así, al leer la etiqueta, {@code T3} dice exactamente qué tubo del proceso
     * es, independientemente de cuántos lotes salgan de él.</p>
     *
     * @param orden posición del tubo dentro del protocolo (1-based)
     */
    public static String primario(String prefijoTubo, String folio, Long idInstitucion, int orden) {
        return prefijo(prefijoTubo) + "/" + folio + "/I" + idInstitucion + "F4-T" + orden;
    }

    /**
     * Etiqueta de una alícuota de un lote. Ej. {@code SR/001104/I1F4-L1/1-12}.
     *
     * <p>La numeración es <b>del lote</b> (continua entre los tubos que lo
     * alimentan), no del tubo: 2 tubos × 6 agrupados dan {@code L1/1-12 … L1/12-12}.
     * {@code pos} es la posición dentro del lote (1-based) y {@code total} el tamaño
     * configurado del lote. De qué tubo primario salió la alícuota no va en la
     * etiqueta: vive en la relación {@code muestraPadre}.</p>
     *
     * @param numeroLote número de lote dentro del (participante, protocolo)
     * @param pos        posición de la alícuota dentro del lote (1-based)
     * @param total      tamaño configurado del lote (suma de alícuotas de sus tubos)
     */
    public static String alicuotaLote(String prefijoTubo, String folio, Long idInstitucion,
                                      int numeroLote, int pos, int total) {
        return prefijo(prefijoTubo) + "/" + folio + "/I" + idInstitucion + "F4-L" + numeroLote
                + "/" + pos + "-" + total;
    }

    /**
     * Etiqueta de una alícuota, colgada de la de su padre. Ej. {@code .../3-5}.
     *
     * <p>Formato heredado; lo usa la carga masiva. El procesamiento por protocolo
     * usa {@link #alicuotaLote}.</p>
     *
     * @param numero      hueco que ocupa dentro del lote, 1-based
     * @param configuradas número de alícuotas que el tubo define
     */
    public static String alicuota(String etiquetaBase, int numero, int configuradas) {
        return etiquetaBase + "/" + numero + "-" + configuradas;
    }
}
