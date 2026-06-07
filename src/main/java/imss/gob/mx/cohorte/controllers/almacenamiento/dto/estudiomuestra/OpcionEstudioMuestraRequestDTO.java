package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OpcionEstudioMuestraRequestDTO {

    @NotBlank(message = "El valor de la opción es requerido")
    @Size(max = 100)
    private String valor;
}
