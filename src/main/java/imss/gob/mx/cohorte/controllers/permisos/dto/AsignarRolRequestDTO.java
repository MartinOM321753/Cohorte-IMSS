package imss.gob.mx.cohorte.controllers.permisos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignarRolRequestDTO {
    @NotNull
    private Long idRol;
}
