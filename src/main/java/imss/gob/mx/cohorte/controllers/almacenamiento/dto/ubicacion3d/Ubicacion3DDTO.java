package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Todo lo que las cuatro vistas del visualizador 3D necesitan, en una llamada.
 *
 * <p>Cuando {@code disponible} es false no se envía escena: el frontend solo
 * muestra el texto de estado (y el préstamo, si lo hay).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DDTO {

    private Ubicacion3DMuestraDTO muestra;

    /** true solo si la muestra tiene posición física visitable en este biobanco. */
    private Boolean disponible;
    /** PRESTADA · SIN_POSICION · BAJA — null cuando disponible es true. */
    private String motivoNoDisponible;
    /** Texto listo para pintar cuando no hay escena. */
    private String mensajeNoDisponible;
    /** Solo cuando motivoNoDisponible == PRESTADA. */
    private Ubicacion3DPrestamoDTO prestamo;

    private Ubicacion3DRefrigeradorDTO refrigerador;
    private Ubicacion3DPisoDTO piso;
    private Ubicacion3DCajaDTO caja;
}
