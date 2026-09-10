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

    /**
     * Cuánto se puede pasar del límite y seguir contando como diferencia menor,
     * en las unidades de este analito. Separa «ligeramente fuera» de «revisar con
     * su médico» en el reporte del participante. Sin valor, el reporte usa una
     * décima parte de la amplitud del rango.
     */
    private Double margenRevision;
    private Boolean activo;
    private String institucionUuid;
    private String institucionNombre;

    /** Alias de columna configurados para la carga masiva. */
    private java.util.List<String> alias;

}
