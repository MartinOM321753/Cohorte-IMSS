package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloRequestDTO {

    @NotBlank(message = "El nombre del protocolo es obligatorio")
    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "Descripción máximo 500 caracteres")
    private String descripcion;

    /** Tipo de muestra que se recolecta y procesa (el origen). */
    @NotNull(message = "El tipo de muestra origen es obligatorio")
    private Long idTipoOrigen;
}
