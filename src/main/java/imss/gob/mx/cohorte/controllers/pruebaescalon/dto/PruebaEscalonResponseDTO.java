package imss.gob.mx.cohorte.controllers.pruebaescalon.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PruebaEscalonResponseDTO {

    private Long id;
    private LocalDate fechaEstudio;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private PacienteResumenDTO paciente;
    private UsuarioResumenDTO usuarioRealiza;
    private List<EtapaResponseDTO> etapas;
}
