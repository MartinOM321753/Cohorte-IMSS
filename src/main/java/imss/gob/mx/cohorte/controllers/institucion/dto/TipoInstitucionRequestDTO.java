package imss.gob.mx.cohorte.controllers.institucion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TipoInstitucionRequestDTO {

    @NotBlank(message = "El nombre del tipo de institución es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
    private String nombre;
}
