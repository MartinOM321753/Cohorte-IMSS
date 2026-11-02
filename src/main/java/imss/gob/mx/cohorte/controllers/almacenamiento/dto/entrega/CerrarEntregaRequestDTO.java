package imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Cuerpo para devolver o marcar consumida una entrega. */
@Data
public class CerrarEntregaRequestDTO {

    @NotNull(message = "Falta el usuario que cierra la entrega")
    private String uuidCierra;

    private String observaciones;
}
