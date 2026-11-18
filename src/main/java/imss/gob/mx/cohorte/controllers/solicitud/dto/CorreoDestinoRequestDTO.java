package imss.gob.mx.cohorte.controllers.solicitud.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CorreoDestinoRequestDTO {

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @Size(max = 140)
    private String nombre;
}
