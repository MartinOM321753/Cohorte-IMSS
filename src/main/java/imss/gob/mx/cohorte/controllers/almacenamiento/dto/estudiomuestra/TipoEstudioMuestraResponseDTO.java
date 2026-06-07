package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoEstudioMuestraResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private List<ParametroEstudioMuestraResponseDTO> parametros;
}
