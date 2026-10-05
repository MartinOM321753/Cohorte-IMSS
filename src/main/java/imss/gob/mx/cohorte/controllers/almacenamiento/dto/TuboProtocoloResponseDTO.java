package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TuboProtocoloResponseDTO {
    private Long id;
    private String nombre;
    private String prefijoCodigo;
    private AccionTubo accion;
    private Integer orden;
    private Boolean activo;
    private Long idTipoResultante;
    private String nombreTipoResultante;
    private Integer numeroAlicuotas;
    private Double volumenAlicuota;
    private List<Double> volumenesAlicuota;
    private String unidadVolumen;
    private String destinoSugerido;
    private Boolean agruparEnLote;
    private Boolean generacionAutomatica;
    private Boolean permiteAlicuotaParcial;
}
