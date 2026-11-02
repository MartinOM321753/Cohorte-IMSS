package imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega;

import lombok.Builder;
import lombok.Data;

/**
 * Opción del selector de destinatario de una entrega: puede ser un colaborador
 * (persona física sin cuenta) o un usuario del sistema ya registrado.
 */
@Data
@Builder
public class DestinatarioEntregaDTO {

    /** "COLABORADOR" o "USUARIO". */
    private String tipo;
    private Long id;
    private String nombreCompleto;
    /** Cargo del colaborador, o nombre de usuario del sistema. */
    private String detalle;
    private boolean esExterno;
}
