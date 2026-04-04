package imss.gob.mx.cohorte.controllers.pruebaescalon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EtapaRequestDTO {

    @NotNull(message = "El ID de la prueba escalón es obligatorio")
    private Long idPruebaEscalon;

    @NotBlank(message = "La etapa es obligatoria")
    @Pattern(regexp = "BASAL|ETAPA_1|ETAPA_2|ETAPA_3", message = "La etapa debe ser BASAL, ETAPA_1, ETAPA_2 o ETAPA_3")
    private String etapa;

    private String observaciones;
}
