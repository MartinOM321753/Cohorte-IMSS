package imss.gob.mx.cohorte.controllers.reportes.dto;

import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlantillaReporteRequestDTO {

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede pasar de 500 caracteres")
    private String descripcion;

    @NotNull(message = "Hay que indicar sobre qué se emite el reporte")
    private TipoReporte tipoReporte;

    /**
     * El diseño, en JSON. Se valida que venga y que sea JSON bien formado; la forma de
     * dentro la comprueba el maquetador al emitir, que es quien sabe qué elementos
     * existen.
     */
    @NotBlank(message = "La plantilla necesita un diseño")
    private String diseno;

    private Boolean predeterminada;
}
