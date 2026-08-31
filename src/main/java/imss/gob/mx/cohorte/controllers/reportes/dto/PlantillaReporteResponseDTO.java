package imss.gob.mx.cohorte.controllers.reportes.dto;

import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlantillaReporteResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private TipoReporte tipoReporte;
    private String diseno;
    private Long idTipoEstudio;
    private String tipoEstudioNombre;
    private Boolean predeterminada;
    private Boolean activo;
    private String institucionNombre;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
