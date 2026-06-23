package imss.gob.mx.cohorte.controllers.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Proyección compacta de un resultado de examen para las gráficas del dashboard.
 * Incluye el valor, la fecha y los rangos de referencia del examen para graficar
 * distribución y comparar con los valores normativos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamenResultGlobalDTO {
    /** Fecha del resultado en formato ISO (YYYY-MM-DDTHH:mm:ss). */
    private String  fecha;
    private String  nombreExamen;
    private String  unidad;
    private Double  valorObtenido;
    private Double  valorMinHombres;
    private Double  valorMaxHombres;
    private Double  valorMinMujeres;
    private Double  valorMaxMujeres;
}
