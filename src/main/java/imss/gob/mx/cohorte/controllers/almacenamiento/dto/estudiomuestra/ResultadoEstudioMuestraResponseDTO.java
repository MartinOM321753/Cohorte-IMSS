package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoEstudioMuestraResponseDTO {

    private Long id;
    /** Nombre del parámetro (no ID, para evitar roundtrips) */
    private String parametro;
    private Long idParametro;
    private Double valorNumerico;
    private String valorTexto;
    private Boolean valorBooleano;
    /** null cuando el grupo es ROOT */
    private String grupoCodigo;
    /** null cuando el grupo es ROOT */
    private String grupoEtiqueta;
    /** null cuando es ROOT (orden 0) */
    private Integer orden;
}
