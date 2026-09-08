package imss.gob.mx.cohorte.controllers.reportes.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * El nombre que se le quiere dar a la copia.
 *
 * <p>Es opcional a propósito: sin él el servidor propone uno libre —«X (copia)», «X
 * (copia 2)»…—. Quien duplica desde el listado no siempre tiene un nombre en mente, y
 * obligarle a inventarlo antes de ver el diseño sobra.</p>
 */
@Data
public class DuplicarPlantillaDTO {

    @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
    private String nombre;
}
