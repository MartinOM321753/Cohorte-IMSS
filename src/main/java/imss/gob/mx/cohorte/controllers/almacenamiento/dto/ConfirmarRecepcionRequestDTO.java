package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConfirmarRecepcionRequestDTO {

    @NotBlank(message = "El UUID del usuario que confirma es obligatorio")
    private String uuidConfirma;
}
