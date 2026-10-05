package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.*;

import java.util.List;

/**
 * Una card de la vista de Lotes: un procesamiento ejecutado = (participante ×
 * protocolo). Lleva sus tubos primarios y sus lotes de alícuotas, con el detalle
 * completo de cada muestra para que el cliente reutilice las mismas tarjetas y
 * acciones (ubicar, imprimir, estudios, traslados…) que el listado anterior.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcesamientoCardResponseDTO {

    private String uuid;
    private String folio;
    private String nombrePaciente;
    private Long idProtocolo;
    private String nombreProtocolo;
    private String tipoOrigen;
    private List<MuestraResponseDTO> tubosPrimarios;
    private List<LoteCardDTO> lotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoteCardDTO {
        private Long idLote;
        private Integer numeroLote;
        private String tipoResultante;
        private List<MuestraResponseDTO> alicuotas;
    }
}
