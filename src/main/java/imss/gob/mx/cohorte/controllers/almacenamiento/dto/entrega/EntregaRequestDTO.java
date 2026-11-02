package imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EntregaRequestDTO {

    @NotEmpty(message = "Debe seleccionar al menos una muestra")
    private List<Long> idsMuestras;

    /** Destinatario colaborador (persona física). Exactamente uno de idColaborador/idUsuario. */
    private Long idColaborador;

    /** Destinatario usuario del sistema. Exactamente uno de idColaborador/idUsuario. */
    private Long idUsuario;

    @NotNull(message = "Falta el usuario que registra la entrega")
    private String uuidEntrega;

    private String motivo;
    private String observaciones;
    private LocalDateTime fechaLimite;
}
