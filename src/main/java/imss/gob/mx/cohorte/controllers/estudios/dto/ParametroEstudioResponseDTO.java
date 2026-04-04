package imss.gob.mx.cohorte.controllers.estudios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroEstudioResponseDTO {

    private Long id;
    private String nombre;
    private String unidad;
    private String tipoEstudio;
}
