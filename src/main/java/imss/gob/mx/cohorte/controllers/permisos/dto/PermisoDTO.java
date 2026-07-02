package imss.gob.mx.cohorte.controllers.permisos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermisoDTO {
    private Long id;
    private String codigo;
    private String modulo;
    private String descripcion;
    private Boolean activo;
}
