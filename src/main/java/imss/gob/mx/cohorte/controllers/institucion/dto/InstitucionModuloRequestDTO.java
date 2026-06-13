package imss.gob.mx.cohorte.controllers.institucion.dto;

import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InstitucionModuloRequestDTO {

    @NotNull(message = "El módulo es obligatorio")
    private ModuloSistema modulo;

    @NotNull(message = "El estado habilitado/deshabilitado es obligatorio")
    private Boolean habilitado;

    /** ID de la institución que otorga/revoca el permiso (debe ser ancestra de la institución destino). */
    @NotNull(message = "La institución otorgante es obligatoria")
    private Long idOtorgante;
}
