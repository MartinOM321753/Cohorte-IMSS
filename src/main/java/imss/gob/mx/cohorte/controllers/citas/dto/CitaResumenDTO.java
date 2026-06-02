package imss.gob.mx.cohorte.controllers.citas.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CitaResumenDTO {
    private String citaUuid;
    private String pacienteUuid;
    private LocalDateTime fecha;
    private String tipo;        // observaciones del médico
    private String estado;      // estadoCita.name()
    private String profesional; // nombre completo del usuarioAgenda
}
