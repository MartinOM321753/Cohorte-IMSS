package imss.gob.mx.cohorte.controllers.estudios.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ParametroEstudioResponseDTO {

    private Long id;
    private String nombre;
    private String unidad;
    private String tipoEstudio;
}
