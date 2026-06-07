package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoMuestraResumenDTO {
    private Long id;
    private String nombre;
    private String temperaturaAlmacenamiento;
}
