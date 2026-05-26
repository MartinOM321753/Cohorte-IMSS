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

    // username ya no se recibe del frontend: el backend lo genera automáticamente
    // a partir de nombre + apellidoPaterno de la persona.

    @NotBlank(message = "El rol es obligatorio")
    private String rolUuid;

    @NotNull(message = "Los datos de persona son obligatorios")
    @Valid
    private PersonaRequestDTO persona;
}
