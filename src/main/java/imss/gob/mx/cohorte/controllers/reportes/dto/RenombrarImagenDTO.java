package imss.gob.mx.cohorte.controllers.reportes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Cómo se llama una imagen en la galería. */
@Data
public class RenombrarImagenDTO {

    @NotBlank(message = "La imagen necesita un nombre")
    @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
    private String nombre;
}
