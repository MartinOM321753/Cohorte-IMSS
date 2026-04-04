package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor

@Getter
@Setter
public class ParametroEstudioRequestDTO {

    @NotNull
    private Long idTipoEstudio;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String unidad;
}
