package imss.gob.mx.cohorte.controllers.users.dto;

import imss.gob.mx.cohorte.utils.validation.MayorDeEdad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonaRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPaterno;

    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @MayorDeEdad(message = "El usuario debe ser mayor de 18 años (tolerancia de 3 meses)")
    private LocalDate fechaNacimiento;

    @NotNull(message = "El sexo es obligatorio")
    @Pattern(regexp = "M|F", message = "Sexo debe ser M o F")
    private String sexo;

    @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos")
    private String telefono;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Email inválido")
    private String email;
}
