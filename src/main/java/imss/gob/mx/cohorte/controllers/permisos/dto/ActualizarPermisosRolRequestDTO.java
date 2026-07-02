package imss.gob.mx.cohorte.controllers.permisos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ActualizarPermisosRolRequestDTO {
    @NotNull
    private List<String> codigosPermisos;
}
