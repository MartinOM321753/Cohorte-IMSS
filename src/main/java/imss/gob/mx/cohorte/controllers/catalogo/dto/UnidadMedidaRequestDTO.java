package imss.gob.mx.cohorte.controllers.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UnidadMedidaRequestDTO {

    @NotBlank(message = "El nombre de la unidad es obligatorio")
    @Size(max = 30, message = "El nombre no puede superar 30 caracteres")
    private String nombre;
}
