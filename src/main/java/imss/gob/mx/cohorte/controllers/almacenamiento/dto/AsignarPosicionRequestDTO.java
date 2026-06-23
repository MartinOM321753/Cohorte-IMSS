package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AsignarPosicionRequestDTO {

    @NotNull(message = "El ID de la posición de caja es obligatorio")
    private Long idPosicionCaja;

    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    private String motivo;
}
