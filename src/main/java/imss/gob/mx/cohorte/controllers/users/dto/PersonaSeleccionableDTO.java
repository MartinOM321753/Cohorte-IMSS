package imss.gob.mx.cohorte.controllers.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO ligero para el selector de tipo de parámetro USUARIO.
 * Combina usuarios del sistema y colaboradores en una sola lista.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaSeleccionableDTO {
    private String nombreCompleto;
    private String username;

    /** "USUARIO" o "COLABORADOR" — indica de dónde viene el registro. */
    private String origen;
}
