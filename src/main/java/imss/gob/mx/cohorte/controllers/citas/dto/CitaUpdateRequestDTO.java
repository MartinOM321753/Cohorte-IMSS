package imss.gob.mx.cohorte.controllers.citas.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CitaUpdateRequestDTO {

    @NotNull(message = "La fecha de cita es obligatoria")
    private LocalDateTime fechaCita;

    @Min(value = 15, message = "La duración mínima es 15 minutos")
    @Max(value = 240, message = "La duración máxima es 240 minutos")
    private Integer duracionMinutos;

    @NotBlank(message = "El estado de la cita es obligatorio")
    private String estadoCita;

    private String observaciones;
}
