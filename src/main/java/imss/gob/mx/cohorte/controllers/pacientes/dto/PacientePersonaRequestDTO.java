package imss.gob.mx.cohorte.controllers.pacientes.dto;

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
public class PacientePersonaRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoPaterno;

    private String apellidoMaterno;

    @MayorDeEdad(message = "El participante debe ser mayor de 18 años (tolerancia de 3 meses)")
    private LocalDate fechaNacimiento;

    @Pattern(regexp = "M|F", message = "Sexo debe ser M o F")
    private String sexo;

    @Pattern(regexp = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d$", message = "El CURP no tiene un formato válido")
    private String curp;

    @Pattern(regexp = "\\d{10}", message = "El teléfono debe tener 10 dígitos")
    private String telefono;

    @Email(message = "Email inválido")
    private String email;
}
