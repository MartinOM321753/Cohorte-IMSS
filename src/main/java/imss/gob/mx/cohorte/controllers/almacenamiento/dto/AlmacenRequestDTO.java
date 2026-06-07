package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.almacen.TipoInstitucion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AlmacenRequestDTO {

    @NotBlank(message = "El nombre de la institución es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

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

    private Boolean activo = true;

    /** Tipo de institución (INMEGEN, INSP, HOSPITAL, LABORATORIO, OTRA). */
    private TipoInstitucion tipo = TipoInstitucion.OTRA;

    /** Indica si la institución tiene su propio biobanco. */
    private Boolean tieneBiobanco = true;

    /** UUID del usuario con rol ENCARGADO asignado a esta institución (obligatorio). */
    @NotBlank(message = "El encargado de la institución es obligatorio")
    private String uuidEncargado;
}
