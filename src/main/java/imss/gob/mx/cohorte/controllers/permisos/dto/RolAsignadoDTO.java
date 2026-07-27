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
public class RolAsignadoDTO {
    private Long idUsuarioRol;
    private Long idRol;
    private String uuid;
    private String nombre;
    private LocalDateTime fechaAsignacion;
}
