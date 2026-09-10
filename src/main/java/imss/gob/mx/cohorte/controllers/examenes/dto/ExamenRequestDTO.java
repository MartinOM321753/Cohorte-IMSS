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

    /**
     * Cuánto se puede pasar del límite y seguir contando como diferencia menor,
     * en las unidades de este analito. Separa «ligeramente fuera» de «revisar con
     * su médico» en el reporte del participante. Sin valor, el reporte usa una
     * décima parte de la amplitud del rango.
     */
    private Double margenRevision;


    /**
     * Nombres con los que los instrumentos titulan la columna de este examen.
     * La lista reemplaza la anterior. Se comparan sin acentos, mayusculas ni
     * espacios de sobra, y dentro de una institucion un alias pertenece a un
     * solo examen.
     */
    private java.util.List<String> alias;

}
