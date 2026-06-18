package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenerarAlicuotasRequestDTO {

    @NotNull(message = "El tipo de muestra es obligatorio")
    private Long idTipoMuestra;

    @NotNull(message = "El tubo de muestra es obligatorio")
    private Long idTuboMuestra;
}
