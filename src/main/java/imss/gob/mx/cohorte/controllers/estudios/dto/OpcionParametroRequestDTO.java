package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpcionParametroRequestDTO {

    @NotBlank(message = "El valor de la opción es obligatorio")
    @Size(max = 100, message = "El valor no puede superar 100 caracteres")
    private String valor;
}
