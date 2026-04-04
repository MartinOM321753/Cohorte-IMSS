package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefrigeradorRequestDTO {

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "Código máximo 50 caracteres")
    private String codigo;

    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    private String nombre;

    @Size(max = 50, message = "Marca máximo 50 caracteres")
    private String marca;

    @Size(max = 50, message = "Modelo máximo 50 caracteres")
    private String modelo;
}
