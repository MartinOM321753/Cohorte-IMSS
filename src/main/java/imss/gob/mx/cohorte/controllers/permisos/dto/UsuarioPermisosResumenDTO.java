package imss.gob.mx.cohorte.controllers.permisos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPermisosResumenDTO {
    private String uuid;
    private String username;
    private String nombreCompleto;
    private List<RolAsignadoDTO> roles;
    private List<PermisoEfectivoDTO> permisosEfectivos;
    private List<PermisoIndividualDTO> permisosIndividuales;
}
