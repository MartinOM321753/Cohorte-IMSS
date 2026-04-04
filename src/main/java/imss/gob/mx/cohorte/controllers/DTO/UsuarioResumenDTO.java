package imss.gob.mx.cohorte.controllers.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResumenDTO {
    private Long id;
    private String username;
    private String UUID;
    private String nombreCompleto;
}
