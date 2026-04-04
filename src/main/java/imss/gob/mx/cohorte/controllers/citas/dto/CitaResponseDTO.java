package imss.gob.mx.cohorte.controllers.citas.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponseDTO {
    private Long id;
    private String estadoCita;
    private LocalDateTime fechaCita;
    private Integer duracionMinutos;
    private String observaciones;
    private LocalDateTime fechaRegistro;
    private PacienteResumenDTO paciente;
    private UsuarioResumenDTO usuarioAgenda;
}
