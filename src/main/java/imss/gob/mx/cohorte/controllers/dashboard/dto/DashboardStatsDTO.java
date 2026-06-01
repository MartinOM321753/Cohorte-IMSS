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
}
