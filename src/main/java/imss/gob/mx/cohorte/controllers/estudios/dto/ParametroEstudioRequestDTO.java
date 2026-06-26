package imss.gob.mx.cohorte.controllers.estudios.dto;

import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ParametroEstudioRequestDTO {

    @NotNull
    private Long idTipoEstudio;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String unidad;

    @NotNull
    private TipoParametro tipo;

    /** Rango de referencia para mujeres (solo aplica a parámetros NUMERICO, opcional). */
    private Double valorMinMujeres;
    private Double valorMaxMujeres;

    /** Rango de referencia para hombres (solo aplica a parámetros NUMERICO, opcional). */
    private Double valorMinHombres;
    private Double valorMaxHombres;

    /**
     * Lista de valores válidos. Solo se usa cuando tipo == TEXTO_OPCIONES.
     * Al crear o actualizar, estas opciones reemplazan las existentes.
     */
    private List<String> opciones;
}
