package imss.gob.mx.cohorte.controllers.reportes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Lo mínimo para renombrar: nombre y descripción, sin el diseño.
 *
 * <p>No es una versión recortada del DTO de actualizar por comodidad. Aquel exige el
 * diseño completo, y renombrar desde el listado obligaría a traérselo y devolverlo:
 * una copia vieja en ese viaje —otra pestaña abierta, una lista sin refrescar— pisaría
 * el guardado bueno sin que nada avisara.</p>
 */
@Data
public class RenombrarPlantillaDTO {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede pasar de 500 caracteres")
    private String descripcion;
}
