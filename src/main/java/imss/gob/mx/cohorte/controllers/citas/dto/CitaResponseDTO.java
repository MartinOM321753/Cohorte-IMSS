package imss.gob.mx.cohorte.controllers.citas.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponseDTO {
    private String uuid;
    private String estadoCita;
    private Instant startAtUtc;
    private Instant endAtUtc;
    private Integer durationMinutes;
    private String timezone;
    private String colorHex;
    private String observaciones;
    private Instant createdAtUtc;
    private PacienteResumenDTO paciente;
    private UsuarioResumenDTO usuarioAgenda;
}
