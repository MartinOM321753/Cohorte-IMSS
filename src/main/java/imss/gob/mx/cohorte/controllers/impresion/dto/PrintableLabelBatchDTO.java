package imss.gob.mx.cohorte.controllers.impresion.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PrintableLabelBatchDTO {
    private ConfiguracionEtiquetaResponseDTO configuracion;
    private List<LabelDataDTO> etiquetas;
}
