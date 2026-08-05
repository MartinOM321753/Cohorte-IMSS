package imss.gob.mx.cohorte.controllers.impresion.dto;

import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.DisposicionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.TipoCodigo;

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
        return entity;
    }
}
