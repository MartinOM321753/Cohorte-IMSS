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
     * @param lote número de lote de esta muestra para ese folio y prefijo
     */
    public static String padre(String prefijoTubo, String folio, Long idInstitucion, int lote) {
        return prefijo(prefijoTubo) + "/" + folio + "/I" + idInstitucion + "F4-L" + lote;
    }

    /**
     * Etiqueta de una alícuota, colgada de la de su padre. Ej. {@code .../3-5}.
     *
     * @param numero      hueco que ocupa dentro del lote, 1-based
     * @param configuradas número de alícuotas que el tubo define
     */
    public static String alicuota(String etiquetaBase, int numero, int configuradas) {
        return etiquetaBase + "/" + numero + "-" + configuradas;
    }
}
