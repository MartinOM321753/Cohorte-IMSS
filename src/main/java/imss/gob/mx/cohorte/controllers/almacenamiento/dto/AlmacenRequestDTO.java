package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AlmacenRequestDTO {

    @NotBlank(message = "El nombre del almacén es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 60, message = "El estado no puede superar 60 caracteres")
    private String estado;

    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 60, message = "La ciudad no puede superar 60 caracteres")
    private String ciudad;

    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    private String direccion;

    @Size(max = 100, message = "El responsable no puede superar 100 caracteres")
    private String responsable;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    private Boolean activo = true;

    /** UUID del usuario con rol ENCARGADO asignado a este almacén (obligatorio). */
    @NotBlank(message = "El encargado del almacén es obligatorio")
    private String uuidEncargado;
}
