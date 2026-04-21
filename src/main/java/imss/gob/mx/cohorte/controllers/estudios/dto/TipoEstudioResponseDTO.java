package imss.gob.mx.cohorte.controllers.estudios.dto;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TipoEstudioResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private List<ParametroEstudio> parametroEstudios;
}
