package imss.gob.mx.cohorte.controllers.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    /** Pacientes con activo = true. */
    private long pacientesActivos;
    /** Citas no canceladas cuyo startAtUtc cae en el mes en curso. */
    private long citasMes;
    /** Citas con estadoCita PROGRAMADA o CONFIRMADA cuya horaFin ya pasó hoy. */
    private long citasSinActualizar;
    /** Estudios médicos con al menos un resultado registrado este mes. */
    private long estudiosConResultadosMes;
    /** Total de resultados de exámenes de laboratorio registrados este mes. */
    private long examenesLabMes;
    /** Total de muestras en biobanco. */
    private long muestrasBiobanco;
    /** Documentos de paciente (consentimientos + generales). */
    private long documentosGenerales;
    /**
     * Variación de pacientesActivos respecto a los de hace 7 días.
     * Positivo = creció, negativo = bajó.
     */
    private int deltasPacientes;
}
