package imss.gob.mx.cohorte.services.importacion;

import java.util.List;

/**
 * Lo que de verdad se escribio.
 *
 * <p>Se devuelve con el detalle por fila y no solo con un total porque quien
 * acaba de cargar cien estudios necesita poder decir cual es cual: sin el folio
 * y el numero de fila, un "97 registrados, 3 omitidos" no permite averiguar
 * cuales fueron los tres.</p>
 */
public record ResultadoCarga(
        int registrados,
        int reemplazados,
        int omitidosPorDuplicado,
        List<Detalle> detalle
) {
    /**
     * @param numeroDeFila el del archivo, para cruzarlo con la hoja de calculo
     * @param accion       REGISTRADO, REEMPLAZADO u OMITIDO
     */
    public record Detalle(int numeroDeFila, String folio, String nombreParticipante,
                          String fecha, String accion, Long idEstudio) {}

    public int total() {
        return registrados + reemplazados + omitidosPorDuplicado;
    }
}
