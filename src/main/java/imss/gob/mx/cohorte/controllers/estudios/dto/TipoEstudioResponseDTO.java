package imss.gob.mx.cohorte.controllers.estudios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoEstudioResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
}
