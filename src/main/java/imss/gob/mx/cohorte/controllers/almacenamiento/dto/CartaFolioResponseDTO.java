package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import lombok.*;

import java.util.List;

/**
 * La «carta» de un participante en biobanco: todo lo que se le hizo, desglosado
 * por tubos primarios y por lotes de alícuotas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartaFolioResponseDTO {

    private String folio;
    private String uuid;
    private String nombrePaciente;
    private List<TuboPrimarioCartaDTO> tubosPrimarios;
    private List<LoteDetalleDTO> lotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TuboPrimarioCartaDTO {
        private Long id;
        private String etiqueta;
        private AccionTubo accion;
        private String nombreTubo;
        private String tipoMuestra;
        private Double valor;
        private String unidad;
        private EstadoMuestra estado;
        private Boolean tienePosicion;
        private String posicionLabel;
        private Boolean agotada;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoteDetalleDTO {
        private Long id;            // id del Lote, o id de la padre si es grupo heredado
        private Integer numeroLote;
        private String tipoResultante;
        private String protocolo;
        private Boolean heredado;   // true = agrupado por padre (sin entidad Lote)
        private List<AlicuotaLoteDTO> alicuotas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlicuotaLoteDTO {
        private Long id;
        private String etiqueta;
        private Integer numeroEnLote;
        private Integer numeroAlicuota;
        private Integer totalAlicuotas;
        private Double valor;
        private String unidad;
        private EstadoMuestra estado;
        private Boolean tienePosicion;
        private String posicionLabel;
        private Boolean materializada;
    }
}
