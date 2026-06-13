package imss.gob.mx.cohorte.controllers.pacientes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("UUID")
    private String UUID;
    private String folio;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private PersonaResponseDTO persona;
    private ReclutamientoParticipanteResponseDTO reclutamiento;
}
