package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstudioAdjuntoRequestDTO {

    @NotBlank
    @Size(max = 50)
    private String tipo;

    @NotBlank
    @Size(max = 255)
    private String nombreOriginal;

    @NotBlank
    @Size(max = 100)
    private String mimeType;

    @NotBlank
    @Size(max = 500)
    private String rutaUrl;

    @Size(max = 255)
    private String descripcion;

    @NotNull
    @Min(0)
    private Integer orden;
}
