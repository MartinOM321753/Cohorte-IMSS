package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoMuestraRequestDTO {

    @NotBlank(message = "El nombre del tipo de muestra es obligatorio")
    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    private String nombre;

    @Size(max = 500, message = "Descripción máximo 500 caracteres")
    private String descripcion;

    @Size(max = 30, message = "Temperatura máximo 30 caracteres")
    private String temperaturaAlmacenamiento;
}
