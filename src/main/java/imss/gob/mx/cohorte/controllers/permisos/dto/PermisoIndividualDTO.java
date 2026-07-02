package imss.gob.mx.cohorte.controllers.permisos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermisoIndividualDTO {
    private Long id;
    private String codigoPermiso;
    private String modulo;
    private String descripcion;
    private String tipo;
    private String motivo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String otorgadoPorUuid;
    private String otorgadoPorUsername;
    private LocalDateTime fechaCreacion;
    private Boolean activo;
}
