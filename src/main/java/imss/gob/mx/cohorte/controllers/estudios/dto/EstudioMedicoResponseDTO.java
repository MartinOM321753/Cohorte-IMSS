package imss.gob.mx.cohorte.controllers.estudios.dto;

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
public class EstudioMedicoResponseDTO {

    private Long id;
    private String observaciones;
    private LocalDate fechaEstudio;
    private LocalDateTime fechaRegistro;
    private PacienteResumenDTO paciente;
    private UsuarioResumenDTO usuarioRealiza;
    private TipoEstudioResponseDTO tipoEstudio;
    private List<ResultadoEstudioResponseDTO> resultados;
    private String institucionUuid;
    private String institucionNombre;
}
