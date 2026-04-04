package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParametroEstudioRequestDTO {

    @NotNull
    private Long idTipoEstudio;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String unidad;
}
