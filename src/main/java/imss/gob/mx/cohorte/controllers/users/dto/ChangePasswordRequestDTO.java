package imss.gob.mx.cohorte.controllers.users.dto;

import imss.gob.mx.cohorte.utils.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Body para PUT /api/users/me/password.
 *
 * passwordActual: requerida solo cuando el usuario ya eligio su propia contraseña antes.
 * nuevaPassword: siempre requerida y debe cumplir la politica fuerte.
 */
public record ChangePasswordRequestDTO(

        String passwordActual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @StrongPassword
        String nuevaPassword
) {}
