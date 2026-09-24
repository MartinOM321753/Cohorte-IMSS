package imss.gob.mx.cohorte.services.importacion;

import java.util.List;

/**
 * Lo que de verdad se escribió.
 *
 * <p>Va con el detalle por fila y no solo con los totales porque quien acaba de
 * cargar seiscientos viales necesita poder decir cuál es cuál: sin la etiqueta y
 * el número de fila, un «619 registrados» no permite cruzarlo con la hoja de
 * cálculo ni ir a buscar un vial concreto a su caja.</p>
 */
public record ResultadoCargaMuestras(

        /** Muestras padre creadas: una por lote. */
        int padresCreadas,

        /** Alícuotas creadas. */
        int alicuotasCreadas,

        /** De esas alícuotas, cuántas quedaron con hueco asignado. */
        int alicuotasUbicadas,

        /** Padres que nacieron ya agotadas porque repartieron todo su volumen. */
        int padresAgotadas,

        List<Detalle> detalle
) {
    /**
     * @param numeroDeFila el del archivo, para cruzarlo con la hoja de cálculo
     * @param posicion     dónde quedó, o null si entró sin hueco
     */
    public record Detalle(int numeroDeFila, String folio, String etiqueta,
                          String tipoMuestra, String tubo,
                          String posicion, Long idMuestra) {}

    public int total() {
        return padresCreadas + alicuotasCreadas;
    }
}
