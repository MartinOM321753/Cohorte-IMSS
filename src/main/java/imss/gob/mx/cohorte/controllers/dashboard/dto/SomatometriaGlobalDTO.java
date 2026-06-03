package imss.gob.mx.cohorte.controllers.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Proyección compacta de un registro de somatometría para las gráficas del dashboard.
 * Solo incluye los campos numéricos necesarios para graficar tendencias agregadas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SomatometriaGlobalDTO {
    /** Fecha de medición en formato ISO (YYYY-MM-DD). */
    private String  fecha;
    private Double  pesoKg;
    private Double  imc;
    private Integer presionSistolica;
    private Integer presionDiastolica;
    private Double  circunferenciaAbdominalCm;
}
