package imss.gob.mx.cohorte.controllers.impresion.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LabelDataDTO {
    private String etiqueta;
    private String nombre;
    private String codigoDatos;
}
