package imss.gob.mx.cohorte.controllers.estudios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoEstudioResponseDTO {

    private Long id;
    private Double valorNumerico;
    private String valorTexto;
    private Boolean valorBooleano;
    private String parametro;
    private String grupoCodigo;
    private String grupoEtiqueta;
    private Integer orden;
}
