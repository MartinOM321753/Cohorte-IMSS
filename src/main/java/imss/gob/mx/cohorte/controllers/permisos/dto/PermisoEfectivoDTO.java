package imss.gob.mx.cohorte.controllers.permisos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermisoEfectivoDTO {
    private String codigo;
    private String modulo;
    private String descripcion;
    private String origen;
    private String detalleOrigen;
}
