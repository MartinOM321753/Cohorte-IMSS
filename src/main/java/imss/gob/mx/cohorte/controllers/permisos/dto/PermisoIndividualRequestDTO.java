package imss.gob.mx.cohorte.controllers.permisos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PermisoIndividualRequestDTO {
    @NotBlank
    private String codigoPermiso;

    private String motivo;

    private LocalDateTime fechaFin;
}
