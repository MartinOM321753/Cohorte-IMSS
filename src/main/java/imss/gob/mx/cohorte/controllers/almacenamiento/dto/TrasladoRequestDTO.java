package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrasladoRequestDTO {

    @NotNull(message = "El ID de la muestra es obligatorio")
    private Long idMuestra;

    @NotNull(message = "El ID del almacén destino es obligatorio")
    private Long idAlmacen;

    @NotBlank(message = "El UUID del usuario que autoriza es obligatorio")
    private String uuidAutoriza;

    @NotBlank(message = "El motivo del traslado es obligatorio")
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    private String motivo;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;
}
