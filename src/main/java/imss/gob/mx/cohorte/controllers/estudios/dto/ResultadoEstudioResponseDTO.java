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
    /**
     * Identificador del parámetro. Es por donde hay que emparejar: el nombre viaja
     * solo para mostrarse. Emparejar por nombre —que es lo que se hacía— desconecta
     * los resultados históricos de su parámetro en cuanto alguien lo renombra, y
     * basta una tilde de diferencia.
     */
    private Long idParametro;

    private String parametro;
    private String grupoCodigo;
    private String grupoEtiqueta;
    private Integer orden;
}
