package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcesarProtocoloRequestDTO {

    @NotBlank(message = "El participante es obligatorio")
    private String pacienteUUID;

    @NotNull(message = "El protocolo es obligatorio")
    private Long idProtocolo;

    private LocalDateTime fechaRecoleccion;

    private String observaciones;

    @Valid
    private List<TuboDecisionRequestDTO> tubos;
}
