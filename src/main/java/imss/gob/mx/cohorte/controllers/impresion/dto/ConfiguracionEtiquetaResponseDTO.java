package imss.gob.mx.cohorte.controllers.impresion.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ConfiguracionEtiquetaResponseDTO {
    private Long id;
    private String nombre;
    private Boolean predeterminada;
    private Double anchoMm;
    private Double altoMm;
    private Integer dpi;
    private Integer etiquetasPorFila;
    private Double margenIzquierdoMm;
    private Double margenSuperiorMm;
    private String tipoCodigo;
    private Integer moduloCodigo;
    private Integer anchoBarraCodigo;
    private Integer tamanoFuenteNombre;
    private Integer tamanoFuenteEtiqueta;
    private Integer espaciadoNombre;
    private Integer espaciadoCodigo;
    private Integer espaciadoEtiqueta;
    private Boolean mostrarNombre;
    private Boolean mostrarCodigo;
    private Boolean mostrarEtiqueta;
    private String disposicion;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private int anchoDots;
    private int altoDots;

    private Integer filasPorPagina;
    private Double espacioHorizontalMm;
    private Double espacioVerticalMm;
    private Double margenPaginaSuperiorMm;
    private Double margenPaginaIzquierdoMm;

    private String tipoMedio;
    private String tamanoHoja;

    /**
     * Medidas de acomodo tal como están guardadas. Cero significa "no capturado"
     * y es un valor con sentido: el formulario de edición devuelve estos campos
     * al backend, así que tienen que viajar crudos. Enviar aquí el valor ya
     * resuelto hacía que la primera edición lo congelara como si alguien lo
     * hubiera capturado, y a partir de entonces cambiar el tamaño o la
     * separación dejaba de mover el acomodo, sin aviso.
     */
    private Double pasoHorizontalMm;
    private Double pasoVerticalMm;
    private Double margenDerechoMm;
    private Double margenInferiorMm;
    private Double ajusteXMm;
    private Double ajusteYMm;

    /**
     * Los mismos valores ya resueltos por la entidad: si el campo crudo está en
     * cero, aquí llega lo que se deducía de tamaño más separación. El frontend
     * dibuja con estos y no tiene que repetir esa decisión.
     */
    private Double pasoHorizontalEfectivoMm;
    private Double pasoVerticalEfectivoMm;
    private Double margenDerechoEfectivoMm;
    private Double margenInferiorEfectivoMm;

    /** Medidas de la hoja, para emitir {@code @page} con tamaño explícito. */
    private Double hojaAnchoMm;
    private Double hojaAltoMm;

    private Integer carrilesRollo;
    private Integer carrilesRolloEfectivo;
    private Double anchoCabezalMm;
    private Integer offsetLhXDots;
    private Integer offsetLhYDots;
}
