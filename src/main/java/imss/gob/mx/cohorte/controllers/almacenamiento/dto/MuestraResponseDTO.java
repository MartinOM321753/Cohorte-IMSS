package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

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
public class MuestraResponseDTO {
    private Long id;
    private String etiqueta;
    private Double valor;
    private String unidad;
    private LocalDateTime fechaRecoleccion;
    private String observaciones;
    private PacienteResumenDTO paciente;
    private UsuarioResumenDTO usuarioRecolecta;
    private UbicacionMuestraDTO ubicacion;
}
