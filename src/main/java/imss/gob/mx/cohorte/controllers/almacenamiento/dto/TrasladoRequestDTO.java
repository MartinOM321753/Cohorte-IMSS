package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class TrasladoRequestDTO {

    /** IDs de las muestras a prestar (padre y/o alícuotas seleccionadas). */
    @NotEmpty(message = "Debe incluir al menos una muestra en el préstamo")
    private List<Long> idsMuestras;

    @NotNull(message = "El ID de la institución destino es obligatorio")
    private Long idInstitucionDestino;

    @NotBlank(message = "El UUID del usuario que autoriza es obligatorio")
    private String uuidAutoriza;

    @NotBlank(message = "El motivo del préstamo es obligatorio")
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    private String motivo;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;
}
