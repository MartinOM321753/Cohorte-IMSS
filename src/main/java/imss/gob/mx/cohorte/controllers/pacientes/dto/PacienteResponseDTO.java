package imss.gob.mx.cohorte.controllers.pacientes.dto;

import imss.gob.mx.cohorte.controllers.DTO.PersonaResponseDTO;
import imss.gob.mx.cohorte.controllers.reclutamiento.dto.ReclutamientoParticipanteResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteResponseDTO {
    private Long id;
    private String uuid;
    private String folio;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private PersonaResponseDTO persona;
    private ReclutamientoParticipanteResponseDTO reclutamiento;

    private Long institucionId;
    private String institucionNombre;
    private Boolean propiaInstitucion;

    /**
     * true → ya no se gestiona a este participante, pero esta institución conserva
     * registros suyos. Se puede consultar lo propio; no registrar ni actualizar.
     */
    private Boolean soloConsulta;
    private Boolean tieneAcceso;
}
