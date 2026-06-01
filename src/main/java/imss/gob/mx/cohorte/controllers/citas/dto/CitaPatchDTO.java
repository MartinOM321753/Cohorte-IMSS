package imss.gob.mx.cohorte.controllers.citas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CitaPatchDTO {
    private String startAtLocal;
    private String timezone;
    
    @Min(value = 15, message = "La duración mínima es 15 minutos")
    @Max(value = 240, message = "La duración máxima es 240 minutos")
    private Integer durationMinutes;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Formato de color hexadecimal inválido")
    private String colorHex;
    
    private String estadoCita;
    private String observaciones;
}
