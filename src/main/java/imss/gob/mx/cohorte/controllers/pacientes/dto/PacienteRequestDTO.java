package imss.gob.mx.cohorte.controllers.pacientes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacienteRequestDTO {

    @NotBlank(message = "El folio es obligatorio")
    @Size(max = 50, message = "Folio máximo 50 caracteres")
    private String folio;

    @NotNull(message = "Los datos de persona son obligatorios")
    @Valid
    private PacientePersonaRequestDTO persona;
}
