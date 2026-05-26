package imss.gob.mx.cohorte.controllers.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body para PUT /api/users/me/password
 *
 * passwordActual: requerida solo cuando el usuario ya eligió su propia contraseña antes.
 *                 Puede ser nula en el primer cambio forzado (debeResetear=true).
 * nuevaPassword:  siempre requerida, mínimo 6 caracteres.
 */
public record ChangePasswordRequestDTO(

        String passwordActual,   // nullable en el primer cambio forzado

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String nuevaPassword
) {}
