package imss.gob.mx.cohorte.controllers.estudios.dto;

import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import lombok.*;

import java.util.List;

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
    private TipoParametro tipo;

    /** Si sigue en uso. Los que no lo están se muestran, pero no se capturan ni se exigen. */
    private Boolean activo;
    private String tipoEstudio;
    private Double valorMinMujeres;
    private Double valorMaxMujeres;
    private Double valorMinHombres;
    private Double valorMaxHombres;

    /**
     * Cuánto se puede pasar del límite y seguir contando como diferencia menor,
     * en las unidades de este parámetro. Separa «ligeramente fuera» de «revisar con
     * su médico» en el reporte del participante. Sin valor, el reporte usa una
     * décima parte de la amplitud del rango.
     */
    private Double margenRevision;

    /** Valores predefinidos. Presente (no null) solo cuando tipo == TEXTO_OPCIONES. */
    private List<String> opciones;

    /** Alias de columna configurados para la carga masiva de resultados. */
    private List<String> alias;
}
