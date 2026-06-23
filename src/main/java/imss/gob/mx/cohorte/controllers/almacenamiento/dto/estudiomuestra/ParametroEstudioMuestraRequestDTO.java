package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ParametroEstudioMuestraRequestDTO {

    @NotNull(message = "El tipo de estudio de muestra es requerido")
    private Long idTipoEstudioMuestra;

    @NotBlank(message = "El nombre del parámetro es requerido")
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String unidad;

    @NotNull(message = "El tipo de parámetro es requerido")
    private TipoParametro tipo;

    private Double valorMinimo;
    private Double valorMaximo;

    /** Solo aplica cuando tipo = TEXTO_OPCIONES */
    private List<String> opciones;
}
