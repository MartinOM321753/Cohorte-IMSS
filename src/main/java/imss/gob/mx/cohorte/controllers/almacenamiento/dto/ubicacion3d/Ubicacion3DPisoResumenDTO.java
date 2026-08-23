package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Plancha de la pila del refrigerador: dimensiones y ocupación precalculadas. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DPisoResumenDTO {
    private Long id;
    private String numeroPiso;
    private Integer filas;
    private Integer columnas;
    private Integer altura;

    private Integer totalPosiciones;
    private Integer posicionesOcupadas;
    private Integer posicionesLibres;
    private Integer porcentajeOcupacion;
    private Integer totalCajas;

    /**
     * Patrón de ocupación de la plancha, aplanado sobre filas × columnas:
     * cuenta de huecos ocupados en esa columna vertical (todas las alturas).
     * Recorrido fila-mayor, longitud = filas × columnas.
     */
    private List<Integer> reticula;

    private Boolean esDestino;
}
