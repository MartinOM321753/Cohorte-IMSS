package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class IniciarDevolucionRequestDTO {

    @NotBlank(message = "El UUID del usuario que inicia la devolución es obligatorio")
    private String uuidInicia;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;

    private List<Long> idsAlicuotasDevolver;

    /**
     * ID de institución destino de la devolución. Si es {@code null}, se devuelve
     * al eslabón anterior de la cadena (comportamiento por defecto). Si viene un ID,
     * debe corresponder a una institución que ya participó en la cadena de custodia
     * de la muestra (dueña original o cualquier tenedor previo).
     */
    private Long idInstitucionDestinoDevolucion;
}
