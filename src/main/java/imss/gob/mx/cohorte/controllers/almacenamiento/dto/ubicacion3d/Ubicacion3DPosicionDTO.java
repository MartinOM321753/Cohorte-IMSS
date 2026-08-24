package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una celda de la rejilla de una caja criogénica.
 *
 * <p>El sistema solo distingue dos estados por celda ({@code ocupada} true/false):
 * no existe una clasificación por tipo de proyecto ni reservas, así que el
 * visualizador tampoco los pinta.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DPosicionDTO {
    private Long id;
    /** Fila 1-based tal como la guarda PosicionCaja. */
    private Integer fila;
    /** Columna 1-based tal como la guarda PosicionCaja. */
    private Integer columna;
    private Boolean ocupada;
    /** Muestra que ocupa la celda; null si está libre. */
    private Long idMuestra;
    private String etiquetaMuestra;
    /** true solo en la celda buscada. Siempre false al explorar sin objetivo. */
    private Boolean esDestino;
}
