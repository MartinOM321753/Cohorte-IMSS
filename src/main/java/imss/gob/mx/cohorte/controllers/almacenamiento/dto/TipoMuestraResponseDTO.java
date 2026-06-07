package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoMuestraResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String temperaturaAlmacenamiento;
    private Boolean activo;
    private List<TuboMuestraResponseDTO> tubos;
}
