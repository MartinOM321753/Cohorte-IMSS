package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoEstudioRequestDTO {

    @NotNull
    private Long idParametro;

    private Double valorNumerico;

    @Size(max = 255)
    private String valorTexto;
}
