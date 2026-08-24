package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Préstamo vigente — se envía en lugar de la escena cuando la muestra está fuera. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DPrestamoDTO {
    private Long idTraslado;
    private String estado;
    private String institucionOrigen;
    private String institucionDestino;
    private String autorizadoPor;
    private LocalDateTime fechaTraslado;
    private LocalDateTime fechaLimite;
    private String motivo;
}
