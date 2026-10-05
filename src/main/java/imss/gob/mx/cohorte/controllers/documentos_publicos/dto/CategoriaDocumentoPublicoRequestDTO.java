package imss.gob.mx.cohorte.controllers.documentos_publicos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoriaDocumentoPublicoRequestDTO {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede exceder 60 caracteres")
    private String nombre;
}
