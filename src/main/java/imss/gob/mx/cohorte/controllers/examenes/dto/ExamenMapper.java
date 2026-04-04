package imss.gob.mx.cohorte.controllers.examenes.dto;

import imss.gob.mx.cohorte.modules.examenes.Examen;

import java.util.List;

public class ExamenMapper {

    public static Examen toEntity(ExamenRequestDTO dto) {
        Examen examen = new Examen();
        examen.setParametro(dto.getNombreExamen());
        examen.setDescripcion(dto.getDescripcion());
        examen.setUnidad(dto.getUnidad());
        examen.setValorMinMujeres(dto.getValorMinMujeres());
        examen.setValorMaxMujeres(dto.getValorMaxMujeres());
        examen.setValorMinHombres(dto.getValorMinHombres());
        examen.setValorMaxHombres(dto.getValorMaxHombres());
        return examen;
    }

    public static ExamenResponseDTO toResponseDTO(Examen e) {
        return ExamenResponseDTO.builder()
            .id(e.getId())
            .nombreExamen(e.getParametro())
            .descripcion(e.getDescripcion())
            .unidad(e.getUnidad())
            .valorMinMujeres(e.getValorMinMujeres())
            .valorMaxMujeres(e.getValorMaxMujeres())
            .valorMinHombres(e.getValorMinHombres())
            .valorMaxHombres(e.getValorMaxHombres())
            .activo(e.getActivo())
            .build();
    }

    public static List<ExamenResponseDTO> toResponseDTOList(List<Examen> list) {
        return list.stream().map(ExamenMapper::toResponseDTO).toList();
    }
}
