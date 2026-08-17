package imss.gob.mx.cohorte.controllers.impresion.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LabelDataDTO {
    /**
     * Identificador de la entidad que origina la etiqueta.
     *
     * Hace falta para el acomodo por carriles: el operador reordena las etiquetas
     * en pantalla y hay que poder decirle al backend qué muestra va en cada
     * carril. Sin esto solo viajaba el texto, que no sirve para identificarla.
     * Nulo cuando la etiqueta no corresponde a una entidad con id propio.
     */
    private Long id;
    private String etiqueta;
    private String nombre;
    private String codigoDatos;
}
