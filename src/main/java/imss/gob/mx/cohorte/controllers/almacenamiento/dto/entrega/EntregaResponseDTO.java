package imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EntregaResponseDTO {

    private Long id;
    private MuestraResumenDTO muestra;
    private DestinatarioResumenDTO destinatario;
    private UsuarioResumenDTO entregadaPor;
    private UsuarioResumenDTO cerradaPor;
    private String estado;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaCierre;
    private LocalDateTime fechaLimite;
    private boolean vencido;
    private String motivo;
    private String observaciones;
    private String grupoEntrega;

    @Data
    @Builder
    public static class MuestraResumenDTO {
        private Long id;
        private String etiqueta;
        private String unidad;
        private Double valor;
        private String estadoMuestra;
        private boolean esAlicuota;
        private String posicionLabel;
    }

    @Data
    @Builder
    public static class DestinatarioResumenDTO {
        /** "COLABORADOR" o "USUARIO". */
        private String tipo;
        private Long id;
        private String nombreCompleto;
        private String cargo;
        private String adscripcion;
        private boolean esExterno;
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
