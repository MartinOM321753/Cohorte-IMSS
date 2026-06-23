package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PisoRefrigeradorRequestDTO {

    @NotBlank(message = "El numero de piso es obligatorio")
    @Size(max = 30, message = "El numero de piso no puede superar 30 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El numero de piso solo puede contener letras, numeros, punto, guion y guion bajo")
    private String numeroPiso;

    @NotNull(message = "El numero de filas es obligatorio")
    @Min(value = 1, message = "El numero de filas debe ser al menos 1")
    @Max(value = 26, message = "El numero de filas no puede superar 26")
    private Integer filas;

    @NotNull(message = "El numero de columnas es obligatorio")
    @Min(value = 1, message = "El numero de columnas debe ser al menos 1")
    @Max(value = 26, message = "El numero de columnas no puede superar 26")
    private Integer columnas;

    @NotNull(message = "La altura es obligatoria")
    @Min(value = 1, message = "La altura debe ser al menos 1")
    @Max(value = 100, message = "La altura no puede superar 100")
    private Integer altura;
}
