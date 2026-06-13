package imss.gob.mx.cohorte.controllers.institucion.dto;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class ParticipanteInstitucionResponseDTO {
    private Long id;
    private Long idPaciente;
    private String pacienteUuid;
    private InstitucionResumenDTO institucion;
    private Boolean activo;
    private String observaciones;
    private Timestamp fechaAsignacion;

    @Data
    @Builder
    public static class InstitucionResumenDTO {
        private Long id;
        private String uuid;
        private String nombre;
    }
}
