package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CajaResponseDTO {
    private Long id;
    private String codigoCaja;
    private Integer filas;
    private Integer columnas;
    private String tipoCaja;
    private String color;
    private String observaciones;
    private Boolean activo;
    private String ubicacionPiso;
}
