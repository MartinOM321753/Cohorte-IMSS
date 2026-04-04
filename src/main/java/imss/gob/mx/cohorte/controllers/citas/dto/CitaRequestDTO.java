package imss.gob.mx.cohorte.controllers.citas.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaRequestDTO {

    @NotBlank(message = "El UUID del paciente es obligatorio")
    private String pacienteUUID;

    @NotBlank(message = "El UUID del usuario que agenda es obligatorio")
    private String usuarioAgendaUUID;

    @NotNull(message = "La fecha de cita es obligatoria")
    @Future(message = "La fecha de cita debe ser futura")
    private LocalDateTime fechaCita;

    @Min(value = 15, message = "La duración mínima es 15 minutos")
    @Max(value = 240, message = "La duración máxima es 240 minutos")
    private Integer duracionMinutos = 60;

    private String observaciones;
}
