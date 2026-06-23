package imss.gob.mx.cohorte.controllers.institucion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ParticipanteInstitucionRequestDTO {

    @NotNull(message = "La institución a vincular es obligatoria")
    private Long idInstitucion;

    @Size(max = 250, message = "Las observaciones no pueden superar 250 caracteres")
    private String observaciones;
}
