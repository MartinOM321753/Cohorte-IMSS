package imss.gob.mx.cohorte.controllers.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendaHoyItemDTO {

    private String  uuid;
    /** Hora de inicio formateada en hora local, ej: "09:30" */
    private String  horaInicio;
    /** Hora de fin formateada en hora local, ej: "10:30" */
    private String  horaFin;
    private Integer duracionMinutos;
    private String  estadoCita;
    private String  colorHex;
    private String  observaciones;
    private PacienteResumen paciente;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PacienteResumen {
        private String folio;
        private String nombreCompleto;
    }
}
