package imss.gob.mx.cohorte.controllers.almacenamiento.dto.colaborador;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ColaboradorResponseDTO {

    private Long id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private LocalDate fechaNacimiento;
    private String sexo;
    private String telefono;
    private String curp;
    private String email;
    private String cargo;
    private String adscripcion;
    private boolean esExterno;
    private boolean activo;
    private LocalDateTime fechaRegistro;
}
