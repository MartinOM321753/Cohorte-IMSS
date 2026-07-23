package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DarDeBajaRequestDTO {

    @NotBlank(message = "El motivo de la baja es obligatorio")
    @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
    private String motivo;
}
