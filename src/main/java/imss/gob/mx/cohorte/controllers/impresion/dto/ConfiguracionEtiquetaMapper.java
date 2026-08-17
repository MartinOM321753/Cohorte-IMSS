package imss.gob.mx.cohorte.controllers.impresion.dto;

import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.DisposicionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.TamanoHoja;
import imss.gob.mx.cohorte.modules.impresion.TipoCodigo;
import imss.gob.mx.cohorte.modules.impresion.TipoMedio;

import java.util.List;

public class ConfiguracionEtiquetaMapper {

    private ConfiguracionEtiquetaMapper() {}

    public static ConfiguracionEtiquetaResponseDTO toResponseDTO(ConfiguracionEtiqueta entity) {
        return ConfiguracionEtiquetaResponseDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .predeterminada(entity.getPredeterminada())
                .anchoMm(entity.getAnchoMm())
                .altoMm(entity.getAltoMm())
                .dpi(entity.getDpi())
                .etiquetasPorFila(entity.getEtiquetasPorFila())
                .margenIzquierdoMm(entity.getMargenIzquierdoMm())
                .margenSuperiorMm(entity.getMargenSuperiorMm())
                .tipoCodigo(entity.getTipoCodigo().name())
                .moduloCodigo(entity.getModuloCodigo())
                .anchoBarraCodigo(entity.getAnchoBarraCodigo())
                .tamanoFuenteNombre(entity.getTamanoFuenteNombre())
                .tamanoFuenteEtiqueta(entity.getTamanoFuenteEtiqueta())
                .espaciadoNombre(entity.getEspaciadoNombre())
                .espaciadoCodigo(entity.getEspaciadoCodigo())
                .espaciadoEtiqueta(entity.getEspaciadoEtiqueta())
                .mostrarNombre(entity.getMostrarNombre())
                .mostrarCodigo(entity.getMostrarCodigo())
                .mostrarEtiqueta(entity.getMostrarEtiqueta())
                .disposicion(entity.getDisposicion().name())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .anchoDots(entity.getAnchoDots())
                .altoDots(entity.getAltoDots())
                .filasPorPagina(entity.getFilasPorPagina())
                .espacioHorizontalMm(entity.getEspacioHorizontalMm())
                .espacioVerticalMm(entity.getEspacioVerticalMm())
                .margenPaginaSuperiorMm(entity.getMargenPaginaSuperiorMm())
                .margenPaginaIzquierdoMm(entity.getMargenPaginaIzquierdoMm())
                .tipoMedio(entity.getTipoMedio().name())
                .tamanoHoja(entity.getTamanoHoja().name())
                // Crudos: son los que el formulario devuelve al guardar.
                .pasoHorizontalMm(entity.getPasoHorizontalMm())
                .pasoVerticalMm(entity.getPasoVerticalMm())
                .margenDerechoMm(entity.getMargenDerechoMm())
                .margenInferiorMm(entity.getMargenInferiorMm())
                .ajusteXMm(entity.getAjusteXMm())
                .ajusteYMm(entity.getAjusteYMm())
                // Resueltos: son los que el frontend usa para dibujar.
                .pasoHorizontalEfectivoMm(entity.getPasoHorizontalMmEfectivo())
                .pasoVerticalEfectivoMm(entity.getPasoVerticalMmEfectivo())
                .margenDerechoEfectivoMm(entity.getMargenDerechoMmEfectivo())
                .margenInferiorEfectivoMm(entity.getMargenInferiorMmEfectivo())
                .hojaAnchoMm(entity.getHojaAnchoMm())
                .hojaAltoMm(entity.getHojaAltoMm())
                .carrilesRollo(entity.getCarrilesRollo())
                .carrilesRolloEfectivo(entity.getCarrilesRolloEfectivo())
                .anchoCabezalMm(entity.getAnchoCabezalMm())
                .offsetLhXDots(entity.getOffsetLhXDots())
                .offsetLhYDots(entity.getOffsetLhYDots())
                .build();
    }

    public static List<ConfiguracionEtiquetaResponseDTO> toResponseDTOList(List<ConfiguracionEtiqueta> entities) {
        return entities.stream().map(ConfiguracionEtiquetaMapper::toResponseDTO).toList();
    }

    public static ConfiguracionEtiqueta toEntity(ConfiguracionEtiquetaRequestDTO dto) {
        ConfiguracionEtiqueta entity = new ConfiguracionEtiqueta();
        entity.setNombre(dto.getNombre());
        entity.setPredeterminada(dto.getPredeterminada() != null ? dto.getPredeterminada() : false);
        entity.setAnchoMm(dto.getAnchoMm());
        entity.setAltoMm(dto.getAltoMm());
        entity.setDpi(dto.getDpi());
        entity.setEtiquetasPorFila(dto.getEtiquetasPorFila());
        entity.setMargenIzquierdoMm(dto.getMargenIzquierdoMm());
        entity.setMargenSuperiorMm(dto.getMargenSuperiorMm());
        entity.setTipoCodigo(TipoCodigo.valueOf(dto.getTipoCodigo()));
        entity.setModuloCodigo(dto.getModuloCodigo());
        entity.setAnchoBarraCodigo(dto.getAnchoBarraCodigo() != null ? dto.getAnchoBarraCodigo() : 2);
        entity.setTamanoFuenteNombre(dto.getTamanoFuenteNombre());
        entity.setTamanoFuenteEtiqueta(dto.getTamanoFuenteEtiqueta());
        entity.setEspaciadoNombre(dto.getEspaciadoNombre() != null ? dto.getEspaciadoNombre() : 4);
        entity.setEspaciadoCodigo(dto.getEspaciadoCodigo() != null ? dto.getEspaciadoCodigo() : 10);
        entity.setEspaciadoEtiqueta(dto.getEspaciadoEtiqueta() != null ? dto.getEspaciadoEtiqueta() : 4);
        entity.setMostrarNombre(dto.getMostrarNombre() != null ? dto.getMostrarNombre() : true);
        entity.setMostrarCodigo(dto.getMostrarCodigo() != null ? dto.getMostrarCodigo() : true);
        entity.setMostrarEtiqueta(dto.getMostrarEtiqueta() != null ? dto.getMostrarEtiqueta() : true);
        entity.setDisposicion(DisposicionEtiqueta.valueOf(dto.getDisposicion()));
        entity.setFilasPorPagina(dto.getFilasPorPagina() != null ? dto.getFilasPorPagina() : 10);
        entity.setEspacioHorizontalMm(dto.getEspacioHorizontalMm() != null ? dto.getEspacioHorizontalMm() : 3.0);
        entity.setEspacioVerticalMm(dto.getEspacioVerticalMm() != null ? dto.getEspacioVerticalMm() : 2.0);
        entity.setMargenPaginaSuperiorMm(dto.getMargenPaginaSuperiorMm() != null ? dto.getMargenPaginaSuperiorMm() : 12.7);
        entity.setMargenPaginaIzquierdoMm(dto.getMargenPaginaIzquierdoMm() != null ? dto.getMargenPaginaIzquierdoMm() : 4.8);
        entity.setTipoMedio(dto.getTipoMedio() != null
                ? TipoMedio.valueOf(dto.getTipoMedio()) : TipoMedio.HOJA_AVERY);
        entity.setTamanoHoja(dto.getTamanoHoja() != null
                ? TamanoHoja.valueOf(dto.getTamanoHoja()) : TamanoHoja.CARTA);
        entity.setPasoHorizontalMm(dto.getPasoHorizontalMm() != null ? dto.getPasoHorizontalMm() : 0.0);
        entity.setPasoVerticalMm(dto.getPasoVerticalMm() != null ? dto.getPasoVerticalMm() : 0.0);
        entity.setMargenDerechoMm(dto.getMargenDerechoMm() != null ? dto.getMargenDerechoMm() : 0.0);
        entity.setMargenInferiorMm(dto.getMargenInferiorMm() != null ? dto.getMargenInferiorMm() : 0.0);
        entity.setAjusteXMm(dto.getAjusteXMm() != null ? dto.getAjusteXMm() : 0.0);
        entity.setAjusteYMm(dto.getAjusteYMm() != null ? dto.getAjusteYMm() : 0.0);
        entity.setCarrilesRollo(dto.getCarrilesRollo() != null ? dto.getCarrilesRollo() : 0);
        entity.setAnchoCabezalMm(dto.getAnchoCabezalMm() != null ? dto.getAnchoCabezalMm() : 104.0);
        entity.setOffsetLhXDots(dto.getOffsetLhXDots() != null ? dto.getOffsetLhXDots() : 0);
        entity.setOffsetLhYDots(dto.getOffsetLhYDots() != null ? dto.getOffsetLhYDots() : 0);
        return entity;
    }
}
