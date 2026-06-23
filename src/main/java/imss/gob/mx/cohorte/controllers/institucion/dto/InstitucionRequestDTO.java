package imss.gob.mx.cohorte.controllers.institucion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InstitucionRequestDTO {

    @NotBlank(message = "El nombre de la institución es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @NotNull(message = "El tipo de institución es obligatorio")
    private Long idTipoInstitucion;

    /** Null indica que esta institución es la raíz del árbol de permisos. */
    private Long idInstitucionPadre;

    @DecimalMin(value = "-90.0", message = "Latitud inválida")
    @DecimalMax(value = "90.0", message = "Latitud inválida")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "Longitud inválida")
    @DecimalMax(value = "180.0", message = "Longitud inválida")
    private Double longitud;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 60, message = "El estado no puede superar 60 caracteres")
    private String estado;

    @NotBlank(message = "La ciudad es obligatoria")
    @Size(max = 60, message = "La ciudad no puede superar 60 caracteres")
    private String ciudad;

    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    private String direccion;

    @Size(max = 100, message = "El responsable no puede superar 100 caracteres")
    private String responsable;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    private String telefono;

    /** UUID del usuario con rol ENCARGADO/ADMINISTRADOR asignado a esta institución (opcional). */
    private String uuidEncargado;

    private Boolean tieneBiobanco = false;

    private Boolean activo = true;
}
