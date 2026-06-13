package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrasladoResponseDTO {

    private Long id;
    private MuestraResumenDTO muestra;
    private InstitucionResumenDTO institucionOrigen;
    private InstitucionResumenDTO institucionDestino;
    private UsuarioResumenDTO autorizadoPor;
    private UsuarioResumenDTO recibidoPor;
    private String estado;
    private LocalDateTime fechaTraslado;
    private LocalDateTime fechaRetorno;
    private String motivo;
    private String observaciones;
    private String grupoTraslado;

    @Data
    @Builder
    public static class MuestraResumenDTO {
        private Long id;
        private String etiqueta;
        private String unidad;
        private String estadoMuestra;
        private boolean esAlicuota;
    }

    @Data
    @Builder
    public static class InstitucionResumenDTO {
        private Long id;
        private String uuid;
        private String nombre;
        private String ciudad;
        private String estado;
    }

    @Data
    @Builder
    public static class UsuarioResumenDTO {
        private Long id;
        private String uuid;
        private String username;
        private String nombreCompleto;
    }
}
