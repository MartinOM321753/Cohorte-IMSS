package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una caja vista desde el piso: su hueco (PosicionPiso) y, si lo hay, la caja
 * que lo ocupa. Los huecos vacíos también viajan para poder dibujar la rejilla
 * completa del piso.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DCajaEnPisoDTO {
    private Long idPosicionPiso;
    /** Etiqueta original de PosicionPiso (letras: A, B, C…). */
    private String fila;
    private String columna;
    /** Altura de PosicionPiso, guardada como número en texto. */
    private String altura;
    /** Índices 1-based derivados de las etiquetas, para posicionar en la escena 3D. */
    private Integer filaIndex;
    private Integer columnaIndex;
    private Integer alturaIndex;

    private Boolean ocupada;

    /** Datos de la caja alojada; null cuando el hueco está libre. */
    private Long idCaja;
    private String codigoCaja;
    private String tipoCaja;
    private String color;
    private Integer capacidad;
    private Integer ocupadas;

    private Boolean esDestino;
}
