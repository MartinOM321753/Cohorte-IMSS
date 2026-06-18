package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MuestraTipoInstitucionResponseDTO {
    private Long id;
    private TipoMuestraResumenDTO tipoMuestra;
    private TuboMuestraResumenDTO tuboMuestra;
    private String nombreInstitucion;
}
