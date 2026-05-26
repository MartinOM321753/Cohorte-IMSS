package imss.gob.mx.cohorte.controllers.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {

    /**
     * Acepta nombre de usuario o correo electrónico.
     */
    @NotBlank(message = "El usuario o correo es requerido")
    @Size(max = 255, message = "El identificador no puede exceder 255 caracteres")
    private String identifier;

    @NotBlank(message = "La contraseña es requerida")
    @Size(max = 255, message = "La contraseña no puede exceder 255 caracteres")
    private String password;
}
