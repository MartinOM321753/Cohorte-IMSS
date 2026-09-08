package imss.gob.mx.cohorte.controllers.reportes.dto;

import imss.gob.mx.cohorte.modules.reportes.PlantillaReporte;

public final class PlantillaReporteMapper {

    private PlantillaReporteMapper() {}

    public static PlantillaReporte toEntity(PlantillaReporteRequestDTO dto) {
        PlantillaReporte p = new PlantillaReporte();
        p.setNombre(dto.getNombre() != null ? dto.getNombre().trim() : null);
        p.setDescripcion(dto.getDescripcion());
        p.setTipoReporte(dto.getTipoReporte());
        p.setDiseno(dto.getDiseno());
        p.setPredeterminada(Boolean.TRUE.equals(dto.getPredeterminada()));
        return p;
    }

    public static PlantillaReporteResponseDTO toResponse(PlantillaReporte p) {
        return PlantillaReporteResponseDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .tipoReporte(p.getTipoReporte())
                .diseno(p.getDiseno())
                .idTipoEstudio(p.getTipoEstudio() != null ? p.getTipoEstudio().getId() : null)
                .tipoEstudioNombre(p.getTipoEstudio() != null ? p.getTipoEstudio().getNombre() : null)
                .predeterminada(p.getPredeterminada())
                .activo(p.getActivo())
                .institucionNombre(p.getInstitucion() != null ? p.getInstitucion().getNombre() : null)
                .fechaCreacion(p.getFechaCreacion())
                .fechaActualizacion(p.getFechaActualizacion())
                .build();
    }

    /**
     * Sin el diseño. Los listados no lo necesitan y un diseño puede pesar bastante:
     * devolverlo en cada fila haría lenta una pantalla que solo muestra nombres.
     */
    public static PlantillaReporteResponseDTO toResumen(PlantillaReporte p) {
        PlantillaReporteResponseDTO dto = toResponse(p);
        dto.setDiseno(null);
        return dto;
    }
}
