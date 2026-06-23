package imss.gob.mx.cohorte.controllers.auth.dto;

import imss.gob.mx.cohorte.utils.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequestDTO(
        @NotBlank(message = "El token es obligatorio")
        String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @StrongPassword
        String nuevaPassword
) {}
