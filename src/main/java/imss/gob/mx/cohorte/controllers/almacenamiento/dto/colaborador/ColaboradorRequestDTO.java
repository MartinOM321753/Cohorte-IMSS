package imss.gob.mx.cohorte.controllers.almacenamiento.dto.colaborador;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Alta/edición de un colaborador. Incluye los datos de su {@code Persona}
 * (el colaborador no requiere cuenta de usuario) más los metadatos del biobanco.
 */
@Data
public class ColaboradorRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 100)
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 100)
    private String apellidoPaterno;

    @Size(max = 100)
    private String apellidoMaterno;

    private LocalDate fechaNacimiento;

    /** "M" o "F". Opcional. */
    private String sexo;

    @Size(max = 10)
    private String telefono;

    @Size(max = 18)
    private String curp;

    @Size(max = 50)
    private String email;

    @Size(max = 150)
    private String cargo;

    @Size(max = 200)
    private String adscripcion;

    private Boolean esExterno;
}
