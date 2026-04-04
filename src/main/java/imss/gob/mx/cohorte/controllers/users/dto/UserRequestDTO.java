package imss.gob.mx.cohorte.controllers.users.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserRequestDTO {

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 4, max = 50, message = "Username entre 4 y 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "Contraseña mínimo 8 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Long idRol;

    @NotNull(message = "Los datos de persona son obligatorios")
    @Valid
    private PersonaRequestDTO persona;
}
