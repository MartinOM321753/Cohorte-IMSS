package imss.gob.mx.cohorte.controllers.institucion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermisoAccesoPacientesResponseDTO {
    private Long id;
    private Long institucionOtorgaId;
    private String institucionOtorgaNombre;
    private Long institucionRecibeId;
    private String institucionRecibeNombre;
    private Boolean habilitado;
    private Timestamp fechaOtorgamiento;
}
