package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
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

    private Boolean valorBooleano;

    @Size(max = 50)
    private String grupoCodigo;

    @Size(max = 100)
    private String grupoEtiqueta;

    @Min(0)
    private Integer orden;
}
