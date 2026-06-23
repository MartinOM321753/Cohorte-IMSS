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

    @NotBlank(message = "El UUID del participante es obligatorio")
    private String pacienteUUID;

    @NotBlank(message = "El UUID del usuario que agenda es obligatorio")
    private String usuarioAgendaUUID;

    @NotBlank(message = "La fecha local es obligatoria")
    private String startAtLocal; // Formato "YYYY-MM-DDTHH:mm"

    @NotBlank(message = "El timezone es obligatorio")
    private String timezone;

    @Min(value = 15, message = "La duración mínima es 15 minutos")
    @Max(value = 240, message = "La duración máxima es 240 minutos")
    private Integer durationMinutes = 60;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Formato de color hexadecimal inválido")
    private String colorHex;

    private String observaciones;
}
