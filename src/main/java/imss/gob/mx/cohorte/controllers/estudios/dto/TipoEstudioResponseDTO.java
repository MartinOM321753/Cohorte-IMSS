package imss.gob.mx.cohorte.controllers.estudios.dto;

import lombok.*;

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
}
