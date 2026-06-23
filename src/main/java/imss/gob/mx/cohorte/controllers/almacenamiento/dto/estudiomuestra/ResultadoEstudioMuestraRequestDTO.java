package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResultadoEstudioMuestraRequestDTO {

    @NotNull(message = "El parámetro es requerido")
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
