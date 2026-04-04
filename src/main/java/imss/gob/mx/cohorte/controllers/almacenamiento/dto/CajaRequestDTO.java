package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CajaRequestDTO {

    @NotBlank(message = "El código de caja es obligatorio")
    @Size(max = 50, message = "Código de caja máximo 50 caracteres")
    private String codigoCaja;

    @NotNull(message = "El número de filas es obligatorio")
    @Min(value = 1, message = "El número de filas debe ser al menos 1")
    private Integer filas;

    @NotNull(message = "El número de columnas es obligatorio")
    @Min(value = 1, message = "El número de columnas debe ser al menos 1")
    private Integer columnas;

    @Size(max = 50, message = "Tipo de caja máximo 50 caracteres")
    private String tipoCaja;

    @Size(max = 30, message = "Color máximo 30 caracteres")
    private String color;

    @Size(max = 500, message = "Observaciones máximo 500 caracteres")
    private String observaciones;

    private Long idPosicionPiso;
}
