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
}
