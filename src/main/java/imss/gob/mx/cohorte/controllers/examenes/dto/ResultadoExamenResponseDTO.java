package imss.gob.mx.cohorte.controllers.examenes.dto;

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
public class ResultadoExamenResponseDTO {
    private Long id;
    private Double valorObtenido;
    private String observaciones;
    private LocalDateTime fechaResultado;
    private PacienteResumenDTO paciente;
    private UsuarioResumenDTO usuarioRegistro;
    private ExamenResponseDTO examen;
    private Boolean dentroDeRango;
}
