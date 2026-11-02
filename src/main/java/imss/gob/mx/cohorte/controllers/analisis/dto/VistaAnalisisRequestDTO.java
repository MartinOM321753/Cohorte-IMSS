package imss.gob.mx.cohorte.controllers.analisis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Alta/edición de una vista guardada. */
@Data
public class VistaAnalisisRequestDTO {

    @NotBlank(message = "La vista necesita un nombre")
    private String nombre;

    @NotBlank(message = "Falta la configuración de la vista")
    private String configuracion;
}
