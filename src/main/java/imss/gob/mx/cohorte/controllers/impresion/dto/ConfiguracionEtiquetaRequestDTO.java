package imss.gob.mx.cohorte.controllers.impresion.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfiguracionEtiquetaRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    private Boolean predeterminada = false;

    @NotNull(message = "El ancho es obligatorio")
    @DecimalMin(value = "10.0", message = "El ancho mínimo es 10mm")
    @DecimalMax(value = "200.0", message = "El ancho máximo es 200mm")
    private Double anchoMm;

    @NotNull(message = "El alto es obligatorio")
    @DecimalMin(value = "10.0", message = "El alto mínimo es 10mm")
    @DecimalMax(value = "200.0", message = "El alto máximo es 200mm")
    private Double altoMm;

    @NotNull(message = "El DPI es obligatorio")
    @Min(value = 150, message = "El DPI mínimo es 150")
    @Max(value = 600, message = "El DPI máximo es 600")
    private Integer dpi;

    @NotNull(message = "Las etiquetas por fila son obligatorias")
    @Min(value = 1, message = "Mínimo 1 etiqueta por fila")
    @Max(value = 12, message = "Máximo 12 etiquetas por fila")
    private Integer etiquetasPorFila;

    @NotNull(message = "El margen izquierdo es obligatorio")
    @DecimalMin(value = "0.0", message = "El margen no puede ser negativo")
    @DecimalMax(value = "20.0", message = "El margen máximo es 20mm")
    private Double margenIzquierdoMm;

    @NotNull(message = "El margen superior es obligatorio")
    @DecimalMin(value = "0.0", message = "El margen no puede ser negativo")
    @DecimalMax(value = "20.0", message = "El margen máximo es 20mm")
    private Double margenSuperiorMm;

    @NotBlank(message = "El tipo de código es obligatorio")
    private String tipoCodigo;

    @NotNull(message = "El módulo del código es obligatorio")
    @Min(value = 1, message = "El módulo mínimo es 1")
    @Max(value = 60, message = "El módulo máximo es 60")
    private Integer moduloCodigo;

    /** Solo se usa en Code 128; el rango es el que admite ^BY en ZPL. */
    @Min(value = 1, message = "El ancho de barra mínimo es 1")
    @Max(value = 10, message = "El ancho de barra máximo es 10")
    private Integer anchoBarraCodigo = 2;

    @NotNull(message = "El tamaño de fuente del nombre es obligatorio")
    @Min(value = 8, message = "El tamaño mínimo es 8")
    @Max(value = 72, message = "El tamaño máximo es 72")
    private Integer tamanoFuenteNombre;

    @NotNull(message = "El tamaño de fuente de la etiqueta es obligatorio")
    @Min(value = 8, message = "El tamaño mínimo es 8")
    @Max(value = 72, message = "El tamaño máximo es 72")
    private Integer tamanoFuenteEtiqueta;

    @Min(value = 0, message = "El espaciado no puede ser negativo")
    @Max(value = 50, message = "El espaciado máximo es 50")
    private Integer espaciadoNombre = 4;

    @Min(value = 0, message = "El espaciado no puede ser negativo")
    @Max(value = 50, message = "El espaciado máximo es 50")
    private Integer espaciadoCodigo = 10;

    @Min(value = 0, message = "El espaciado no puede ser negativo")
    @Max(value = 50, message = "El espaciado máximo es 50")
    private Integer espaciadoEtiqueta = 4;

    private Boolean mostrarNombre = true;

    private Boolean mostrarCodigo = true;

    private Boolean mostrarEtiqueta = true;

    @NotBlank(message = "La disposición es obligatoria")
    private String disposicion;

    @Min(value = 1, message = "Mínimo 1 fila por página")
    @Max(value = 30, message = "Máximo 30 filas por página")
    private Integer filasPorPagina = 10;

    @DecimalMin(value = "0.0", message = "El espacio no puede ser negativo")
    @DecimalMax(value = "50.0", message = "El espacio máximo es 50mm")
    private Double espacioHorizontalMm = 3.0;

    @DecimalMin(value = "0.0", message = "El espacio no puede ser negativo")
    @DecimalMax(value = "50.0", message = "El espacio máximo es 50mm")
    private Double espacioVerticalMm = 2.0;

    @DecimalMin(value = "0.0", message = "El margen no puede ser negativo")
    @DecimalMax(value = "50.0", message = "El margen máximo es 50mm")
    private Double margenPaginaSuperiorMm = 12.7;

    @DecimalMin(value = "0.0", message = "El margen no puede ser negativo")
    @DecimalMax(value = "50.0", message = "El margen máximo es 50mm")
    private Double margenPaginaIzquierdoMm = 4.8;

    private String tipoMedio = "HOJA_AVERY";

    private String tamanoHoja = "CARTA";

    /**
     * Paso entre etiquetas. Cero es válido y significa "dedúcelo de tamaño más
     * separación", que es como se comportaba antes de que el campo existiera.
     */
    @DecimalMin(value = "0.0", message = "El paso no puede ser negativo")
    @DecimalMax(value = "250.0", message = "El paso máximo es 250mm")
    private Double pasoHorizontalMm = 0.0;

    @DecimalMin(value = "0.0", message = "El paso no puede ser negativo")
    @DecimalMax(value = "250.0", message = "El paso máximo es 250mm")
    private Double pasoVerticalMm = 0.0;

    @DecimalMin(value = "0.0", message = "El margen no puede ser negativo")
    @DecimalMax(value = "20.0", message = "El margen máximo es 20mm")
    private Double margenDerechoMm = 0.0;

    @DecimalMin(value = "0.0", message = "El margen no puede ser negativo")
    @DecimalMax(value = "20.0", message = "El margen máximo es 20mm")
    private Double margenInferiorMm = 0.0;

    /** La calibración corrige en los dos sentidos, así que admite negativos. */
    @DecimalMin(value = "-20.0", message = "El ajuste mínimo es -20mm")
    @DecimalMax(value = "20.0", message = "El ajuste máximo es 20mm")
    private Double ajusteXMm = 0.0;

    @DecimalMin(value = "-20.0", message = "El ajuste mínimo es -20mm")
    @DecimalMax(value = "20.0", message = "El ajuste máximo es 20mm")
    private Double ajusteYMm = 0.0;

    @Min(value = 0, message = "Los carriles no pueden ser negativos")
    @Max(value = 12, message = "Máximo 12 carriles")
    private Integer carrilesRollo = 0;

    @DecimalMin(value = "10.0", message = "El ancho de cabezal mínimo es 10mm")
    @DecimalMax(value = "300.0", message = "El ancho de cabezal máximo es 300mm")
    private Double anchoCabezalMm = 104.0;

    @Min(value = -600, message = "El desplazamiento mínimo es -600 dots")
    @Max(value = 600, message = "El desplazamiento máximo es 600 dots")
    private Integer offsetLhXDots = 0;

    @Min(value = -600, message = "El desplazamiento mínimo es -600 dots")
    @Max(value = 600, message = "El desplazamiento máximo es 600 dots")
    private Integer offsetLhYDots = 0;

    /**
     * El paso tiene que dar cabida a la etiqueta: un paso menor al tamaño hace
     * que las etiquetas se encimen, y es un error de captura fácil de cometer al
     * confundir paso con separación.
     */
    @AssertTrue(message = "El paso vertical no puede ser menor que el alto de la etiqueta")
    public boolean isPasoVerticalCoherente() {
        if (pasoVerticalMm == null || pasoVerticalMm == 0.0 || altoMm == null) return true;
        return pasoVerticalMm >= altoMm;
    }

    @AssertTrue(message = "El paso horizontal no puede ser menor que el ancho de la etiqueta")
    public boolean isPasoHorizontalCoherente() {
        if (pasoHorizontalMm == null || pasoHorizontalMm == 0.0 || anchoMm == null) return true;
        return pasoHorizontalMm >= anchoMm;
    }

    /** Los márgenes internos no pueden comerse el área útil por completo. */
    @AssertTrue(message = "Los márgenes internos no dejan área útil dentro de la etiqueta")
    public boolean isAreaUtilPositiva() {
        if (anchoMm == null || altoMm == null) return true;
        double der = (margenDerechoMm != null && margenDerechoMm > 0)
                ? margenDerechoMm
                : (margenIzquierdoMm != null ? margenIzquierdoMm : 0.0);
        double inf = margenInferiorMm != null ? margenInferiorMm : 0.0;
        double izq = margenIzquierdoMm != null ? margenIzquierdoMm : 0.0;
        double sup = margenSuperiorMm != null ? margenSuperiorMm : 0.0;
        return (anchoMm - izq - der) > 0 && (altoMm - sup - inf) > 0;
    }

    /**
     * El QR se genera con {@code ^BQN,2,<modulo>}, y la magnificacion de ^BQ solo
     * admite de 1 a 10: por encima de eso el ZPL sale fuera de rango y la Zebra no
     * imprime lo que se configuro. Los demas codigos usan el tope general.
     */
    @AssertTrue(message = "Para código QR el módulo máximo es 10")
    public boolean isModuloParaTipoCodigo() {
        if (moduloCodigo == null || tipoCodigo == null) return true;
        if (!"QR_CODE".equals(tipoCodigo)) return true;
        return moduloCodigo <= 10;
    }
}
