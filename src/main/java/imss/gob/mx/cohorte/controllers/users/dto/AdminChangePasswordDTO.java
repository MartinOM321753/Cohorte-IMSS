package imss.gob.mx.cohorte.controllers.users.dto;

import imss.gob.mx.cohorte.utils.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record AdminChangePasswordDTO(
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @StrongPassword
        String nuevaPassword
) {}
