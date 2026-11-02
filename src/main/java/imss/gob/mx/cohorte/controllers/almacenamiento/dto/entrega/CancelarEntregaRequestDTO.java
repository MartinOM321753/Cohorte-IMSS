package imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelarEntregaRequestDTO {

    @NotNull(message = "Falta el usuario que cancela la entrega")
    private String uuidUsuario;

    private String motivo;
}
