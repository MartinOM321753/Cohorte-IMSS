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
    private LocalDateTime fechaLimite;
    private boolean vencido;
    private String motivo;
    private String observaciones;
    private String grupoTraslado;
    /**
     * Que forma tiene esta fila en EN_DEVOLUCION.
     *
     * <p>Viaja al frontend porque sin ella la pantalla no puede saber quien
     * envia y quien recibe: en un prestamo de ida el tenedor esta en
     * institucionDestino, y en un movimiento de devolucion —el que crea la
     * propia devolucion para las alicuotas— esta en institucionOrigen. Leer las
     * dos igual invertia las etiquetas y ofrecia el boton de confirmar a quien
     * manda la muestra.</p>
     */
    private boolean esMovimientoDevolucion;
    /**
     * Atajo de la devolucion: a donde vuelve la muestra en vez de al prestador
     * original. Va al frontend por lo mismo que el campo de arriba — sin el, la
     * pantalla calcularia que confirma el origen cuando en realidad confirma el
     * tercero, y ofreceria el boton a quien recibiria un 403.
     */
    private Long idInstitucionDestinoDevolucion;

    @Data
    @Builder
    public static class MuestraResumenDTO {
        private Long id;
        private String etiqueta;
        private String unidad;
        private String estadoMuestra;
        private boolean esAlicuota;
        private String posicionLabel;
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
