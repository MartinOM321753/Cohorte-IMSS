package imss.gob.mx.cohorte.controllers.examenes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamenResponseDTO {
    private Long id;
    private String nombreExamen;
    private String descripcion;
    private String unidad;
    private Double valorMinMujeres;
    private Double valorMaxMujeres;
    private Double valorMinHombres;
    private Double valorMaxHombres;
    private Boolean activo;
}
