package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TipoEstudioMuestraRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100)
    private String nombre;

    @Size(max = 500)
    private String descripcion;
}
