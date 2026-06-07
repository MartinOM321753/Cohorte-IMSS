package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroEstudioMuestraResponseDTO {

    private Long id;
    private Long idTipoEstudioMuestra;
    private String nombreTipoEstudioMuestra;
    private String nombre;
    private String unidad;
    private TipoParametro tipo;
    private Double valorMinimo;
    private Double valorMaximo;
    /** Poblado solo cuando tipo = TEXTO_OPCIONES */
    private List<String> opciones;
}
