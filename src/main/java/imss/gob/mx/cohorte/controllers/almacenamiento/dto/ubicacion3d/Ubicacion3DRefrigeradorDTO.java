package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Refrigerador con su pila de pisos.
 *
 * <p>La entidad no guarda estado operativo ni temperatura: solo el interruptor
 * {@code activo}. El panel del visualizador refleja exactamente eso.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DRefrigeradorDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String marca;
    private String modelo;
    private Boolean activo;
    private String nombreInstitucion;

    private Integer totalPisos;
    private Integer totalPosiciones;
    private Integer posicionesOcupadas;
    private Integer porcentajeOcupacion;

    /** Ordenados por numeroPiso; el índice 0 es la plancha inferior. */
    private List<Ubicacion3DPisoResumenDTO> pisos;

    /** Null al explorar sin muestra objetivo. */
    private Long idPisoDestino;
}
