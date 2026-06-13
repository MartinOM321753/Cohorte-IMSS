package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CancelarPrestamoRequestDTO {

    @NotBlank(message = "El UUID del usuario que cancela el préstamo es obligatorio")
    private String uuidUsuario;

    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    private String motivo;
}
