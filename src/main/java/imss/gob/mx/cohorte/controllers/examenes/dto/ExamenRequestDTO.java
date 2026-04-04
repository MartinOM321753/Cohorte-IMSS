package imss.gob.mx.cohorte.controllers.examenes.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamenRequestDTO {

    @jakarta.validation.constraints.NotBlank(message = "El nombre del examen es obligatorio")
    @Size(max = 100, message = "Nombre del examen máximo 100 caracteres")
    private String nombreExamen;

    @Size(max = 500, message = "Descripción máximo 500 caracteres")
    private String descripcion;

    @Size(max = 10, message = "Unidad máximo 10 caracteres")
    private String unidad;

    private Double valorMinMujeres;
    private Double valorMaxMujeres;
    private Double valorMinHombres;
    private Double valorMaxHombres;
}
