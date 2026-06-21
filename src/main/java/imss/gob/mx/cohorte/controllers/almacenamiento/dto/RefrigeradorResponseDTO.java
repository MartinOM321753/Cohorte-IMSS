package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefrigeradorResponseDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String marca;
    private String modelo;
    private Boolean activo;
    private int totalPisos;
    private List<PisoResumenDTO> pisos;
}
