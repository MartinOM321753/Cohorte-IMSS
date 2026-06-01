package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrasladoResponseDTO {

    private Long id;
    private MuestraResumenDTO muestra;
    private AlmacenResponseDTO almacen;
    private UsuarioAutorizaDTO autorizadoPor;
    private String estado;
    private LocalDateTime fechaTraslado;
    private LocalDateTime fechaRetorno;
    private String motivo;
    private String observaciones;

    @Data
    @Builder
    public static class MuestraResumenDTO {
        private Long id;
        private String etiqueta;
        private String unidad;
    }

    @Data
    @Builder
    public static class UsuarioAutorizaDTO {
        private Long id;
        private String uuid;
        private String username;
        private String nombreCompleto;
    }
}
