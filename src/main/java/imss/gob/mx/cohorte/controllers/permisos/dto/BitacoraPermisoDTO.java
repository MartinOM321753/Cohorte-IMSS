package imss.gob.mx.cohorte.controllers.permisos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraPermisoDTO {
    private Long id;
    private String usuarioAfectadoUuid;
    private String usuarioAfectadoNombre;
    private String accion;
    private String detalle;
    private String realizadoPorUuid;
    private String realizadoPorNombre;
    private LocalDateTime timestamp;
}
