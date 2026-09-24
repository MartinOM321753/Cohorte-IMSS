package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * El orden nuevo de los parámetros de un tipo de estudio de muestra, como la
 * lista completa de sus identificadores de la primera posición a la última.
 * Mismo contrato que el del catálogo de estudios.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ReordenarParametrosEstudioMuestraRequestDTO {

    @NotEmpty(message = "Hay que mandar la lista de parámetros en el orden deseado")
    private List<Long> ids;
}
