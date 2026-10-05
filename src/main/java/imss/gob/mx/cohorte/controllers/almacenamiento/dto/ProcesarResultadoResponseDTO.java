package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcesarResultadoResponseDTO {

    private Long idPaciente;
    private String folio;
    private Integer numeroPadres;
    private Integer numeroAlicuotas;
    private List<PadreBreveDTO> padres;
    private List<LoteResumenDTO> lotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PadreBreveDTO {
        private Long id;
        private String etiqueta;
        private AccionTubo accion;
        private String nombreTubo;
        private Double valor;
        private String unidad;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoteResumenDTO {
        private Long id;
        private Integer numeroLote;
        private Long idTipoResultante;
        private String nombreTipoResultante;
        private Integer numeroAlicuotas;
    }
}
