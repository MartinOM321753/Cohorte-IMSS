package imss.gob.mx.cohorte.controllers.pacientes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Una opción del desplegable de institución al registrar un participante.
 *
 * <p>{@code visible} indica si el usuario seguirá viendo al participante después
 * de guardarlo. Puede ser {@code false} al registrar para una institución
 * hermana: la autorización permite darla de alta ahí, pero no abre el padrón de
 * esa sede. El formulario lo advierte antes de guardar en vez de dejar que el
 * registro desaparezca sin explicación.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitucionRegistroOpcionDTO {
    private Long id;
    private String nombre;
    /** Es la institución del usuario autenticado; se preselecciona. */
    private Boolean propia;
    /** El participante seguirá apareciendo en los listados del usuario. */
    private Boolean visible;
}
