package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * El orden nuevo de los parámetros de un tipo de estudio, como la lista completa
 * de sus identificadores de la primera posición a la última.
 *
 * <p>Se manda la lista entera y no el movimiento («este parámetro se fue de la
 * posición 3 a la 1») porque el movimiento hay que interpretarlo contra un estado
 * que pudo cambiar mientras la pantalla estaba abierta, y entonces reacomoda algo
 * distinto de lo que el usuario vio. La lista describe el resultado, no el camino:
 * o coincide con el catálogo y se aplica tal cual, o no coincide y se rechaza.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReordenarParametrosRequestDTO {

    /** Ids de todos los parámetros del tipo, en el orden deseado. */
    @NotEmpty(message = "Hay que mandar la lista de parámetros en el orden deseado")
    private List<Long> ids;
}
