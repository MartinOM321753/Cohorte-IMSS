package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Piso destino con la rejilla real de huecos y las cajas que los ocupan. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DPisoDTO {
    private Long id;
    private String numeroPiso;
    /** Dimensiones reales de la rejilla del piso — configurables por piso. */
    private Integer filas;
    private Integer columnas;
    private Integer altura;

    private Integer totalPosiciones;
    private Integer posicionesOcupadas;
    private Integer posicionesLibres;
    private Integer porcentajeOcupacion;
    private Integer totalCajas;

    private List<Ubicacion3DCajaEnPisoDTO> cajas;

    private Long idRefrigerador;
    private String codigoRefrigerador;

    /** Todo lo que sigue es null al explorar sin muestra objetivo. */
    private Long idCajaDestino;
    private Integer filaDestinoIndex;
    private Integer columnaDestinoIndex;
    private Integer alturaDestinoIndex;
}
