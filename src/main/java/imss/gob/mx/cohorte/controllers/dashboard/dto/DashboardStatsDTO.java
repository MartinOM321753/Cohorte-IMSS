package imss.gob.mx.cohorte.controllers.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long pacientesActivos;
    private long citasProgramadas;
    private long muestrasBiobanco;
    /** Estudios médicos que tienen al menos un resultado registrado. */
    private long estudiosConResultados;
    /** Total de resultados de exámenes de laboratorio registrados. */
    private long examenesLab;
    /** Documentos de paciente (consentimientos + generales). */
    private long documentosGenerales;
    /** Documentos vinculados a muestras biológicas. */
    private long documentosMuestra;
}
