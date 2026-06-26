package imss.gob.mx.cohorte.controllers.estudios.dto;

import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ParametroEstudioResponseDTO {

    private Long id;
    private String nombre;
    private String unidad;
    private TipoParametro tipo;
    private String tipoEstudio;
    private Double valorMinMujeres;
    private Double valorMaxMujeres;
    private Double valorMinHombres;
    private Double valorMaxHombres;

    /** Valores predefinidos. Presente (no null) solo cuando tipo == TEXTO_OPCIONES. */
    private List<String> opciones;
}
