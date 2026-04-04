package imss.gob.mx.cohorte.controllers.pruebaescalon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicionRequestDTO {

    @NotNull(message = "El ID de la etapa es obligatorio")
    private Long idEtapa;

    @NotBlank(message = "El parámetro es obligatorio")
    @Pattern(regexp = "PULSO|TA_SISTOLICA|TA_DIASTOLICA|FCM", message = "El parámetro debe ser PULSO, TA_SISTOLICA, TA_DIASTOLICA o FCM")
    private String parametro;

    @NotNull(message = "El valor es obligatorio")
    private Double valor;

    private String unidad;
}
