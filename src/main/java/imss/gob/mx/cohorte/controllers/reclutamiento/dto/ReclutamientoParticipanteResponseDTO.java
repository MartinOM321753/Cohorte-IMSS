package imss.gob.mx.cohorte.controllers.reclutamiento.dto;

import imss.gob.mx.cohorte.modules.reclutamiento.EstadoContacto;
import imss.gob.mx.cohorte.modules.reclutamiento.MedioContacto;
import imss.gob.mx.cohorte.modules.reclutamiento.TipoReclutamiento;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class ReclutamientoParticipanteResponseDTO {
    private Long id;
    private TipoReclutamiento tipoReclutamiento;
    private EstadoContacto estadoContacto;
    private MedioContacto medioContacto;
    private InstitucionResumenDTO institucionReclutamiento;
    private UsuarioResumenDTO usuarioRecluta;
    private String observaciones;
    private Timestamp fechaContacto;
    private Timestamp fechaRegistro;

    @Data
    @Builder
    public static class InstitucionResumenDTO {
        private Long id;
        private String uuid;
        private String nombre;
    }

    @Data
    @Builder
    public static class UsuarioResumenDTO {
        private Long id;
        private String uuid;
        private String nombreCompleto;
    }
}
