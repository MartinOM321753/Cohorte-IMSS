package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Caja criogénica destino con su rejilla completa de posiciones. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DCajaDTO {
    private Long id;
    private String codigoCaja;
    /** Dimensiones reales de la rejilla — configurables por caja. */
    private Integer filas;
    private Integer columnas;
    /** Catálogo libre de texto en CajaCriogenica.tipoCaja; puede venir null. */
    private String tipoCaja;
    private String color;
    private String observaciones;

    private Integer capacidad;
    private Integer ocupadas;
    private Integer libres;
    private Integer porcentajeOcupacion;

    private List<Ubicacion3DPosicionDTO> posiciones;

    /** Coordenada destino dentro de esta caja. Null al explorar sin objetivo. */
    private Integer filaDestino;
    private Integer columnaDestino;

    /**
     * Dónde vive la caja. Permite al explorador dibujar la ruta de regreso
     * cuando se entra directamente por la caja, sin haber pasado por el piso.
     */
    private Long idPiso;
    private String numeroPiso;
    private Long idRefrigerador;
    private String codigoRefrigerador;
    /** Coordenada de la caja dentro de su piso; null si aún no está colocada. */
    private String coordenadaEnPiso;
}
