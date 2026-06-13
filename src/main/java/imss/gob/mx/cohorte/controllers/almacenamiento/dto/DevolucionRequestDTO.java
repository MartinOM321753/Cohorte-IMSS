package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DevolucionRequestDTO {

    @NotBlank(message = "El UUID del usuario que confirma la devolución es obligatorio")
    private String uuidConfirma;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;
}
