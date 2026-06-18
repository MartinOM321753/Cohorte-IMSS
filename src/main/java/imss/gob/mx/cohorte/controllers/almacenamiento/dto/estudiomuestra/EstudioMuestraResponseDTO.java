package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstudioMuestraResponseDTO {

    private Long id;
    private Long idMuestra;
    private String etiquetaMuestra;
    private TipoEstudioMuestraResponseDTO tipoEstudioMuestra;
    private UsuarioResumenDTO usuarioRealiza;
    private LocalDate fechaEstudio;
    private LocalDateTime fechaRegistro;
    private String observaciones;
    private Double cantidadConsumida;
    private String unidadConsumida;
    private List<ResultadoEstudioMuestraResponseDTO> resultados;
    private Integer cantidadResultados;
    private Long idInstitucionCreadora;
}
