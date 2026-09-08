package imss.gob.mx.cohorte.controllers.reportes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FormulaReporteRequestDTO {

    @NotBlank(message = "La fórmula necesita un nombre")
    @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede pasar de 500 caracteres")
    private String descripcion;

    /** Lo escrito: {@code peso / estatura²}. Que se pueda leer lo revisa el servicio. */
    @NotBlank(message = "La fórmula necesita una expresión")
    @Size(max = 2000, message = "La expresión no puede pasar de 2000 caracteres")
    private String expresion;

    /** Las variables que usa, cada una con su origen y la unidad en que entra. */
    private List<VariableFormulaDTO> variables;

    /**
     * Los límites de la referencia, cuando dependen del participante.
     *
     * <p>Se escriben con las mismas variables que la expresión principal. Son
     * opcionales y van por separado porque hay filas con un solo lado: «&lt;200» no
     * tiene mínimo y «≥50» no tiene máximo.</p>
     */
    @Size(max = 2000, message = "El límite mínimo no puede pasar de 2000 caracteres")
    private String expresionMinimo;

    @Size(max = 2000, message = "El límite máximo no puede pasar de 2000 caracteres")
    private String expresionMaximo;

    /**
     * En qué sale el resultado. Texto libre porque hay resultados cuya unidad no está
     * en ningún catálogo: kg/m² es el caso obvio.
     */
    @Size(max = 40, message = "La unidad no puede pasar de 40 caracteres")
    private String unidadSalida;

    /** Con cuántos decimales se imprime. Sin indicar: los que traiga el resultado. */
    @Min(value = 0, message = "Los decimales no pueden ser negativos")
    @Max(value = 10, message = "Diez decimales es el máximo")
    private Integer decimales;

    @Data
    public static class VariableFormulaDTO {

        /** Cómo la menciona la fórmula: «estatura». */
        @NotBlank(message = "Cada variable necesita un nombre")
        @Size(max = 60, message = "El nombre de la variable no puede pasar de 60 caracteres")
        private String nombre;

        /**
         * De dónde sale el dato, en el catálogo de campos: {@code estudio.7.param.85}.
         *
         * <p>Se guarda la clave y no el nombre del parámetro para que la fórmula
         * sobreviva a que alguien renombre el estudio, que es lo que pasa con el
         * tiempo.</p>
         */
        @NotBlank(message = "Cada variable necesita saber de dónde sale")
        @Size(max = 120, message = "La clave no puede pasar de 120 caracteres")
        private String clave;

        /**
         * En qué unidad entra al cálculo.
         *
         * <p>Es la decisión que evita el error que nada delata: la estatura está
         * guardada en centímetros y la fórmula del índice de masa corporal la necesita
         * en metros. Sin indicar, el dato entra tal como está guardado.</p>
         */
        @Size(max = 40, message = "La unidad no puede pasar de 40 caracteres")
        private String unidad;
    }
}
