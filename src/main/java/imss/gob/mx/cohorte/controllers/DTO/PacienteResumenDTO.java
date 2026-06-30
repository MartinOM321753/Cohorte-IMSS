package imss.gob.mx.cohorte.controllers.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteResumenDTO {
    private Long id;
    private String uuid;
    private String folio;
    private String nombreCompleto;
    private String sexo;
}
