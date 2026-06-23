package imss.gob.mx.cohorte.controllers.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("UUID")
    private String UUID;
    private String folio;
    private String nombreCompleto;
    private String sexo;
}
