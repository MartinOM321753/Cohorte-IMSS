package imss.gob.mx.cohorte.controllers.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDTO(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingresa un correo válido")
        String email
) {}
