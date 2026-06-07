package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TuboMuestraResponseDTO {
    private Long id;
    private String nombre;
    private String prefijoCodigo;
    private Integer numeroAlicuotas;
    private Double volumenAlicuota;
    private String unidadVolumen;
    private String destinoSugerido;
    private Integer orden;
    private Boolean activo;
}
