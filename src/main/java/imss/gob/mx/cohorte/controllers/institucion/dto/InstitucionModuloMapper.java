package imss.gob.mx.cohorte.controllers.institucion.dto;

import imss.gob.mx.cohorte.modules.institucion.InstitucionModulo;

import java.util.List;

public class InstitucionModuloMapper {

    private InstitucionModuloMapper() {}

    public static InstitucionModuloResponseDTO toResponseDTO(InstitucionModulo m) {
        return InstitucionModuloResponseDTO.builder()
                .id(m.getId())
                .idInstitucion(m.getInstitucion().getId())
                .nombreInstitucion(m.getInstitucion().getNombre())
                .modulo(m.getModulo())
                .habilitado(m.getHabilitado())
                .idOtorgadoPor(m.getOtorgadoPor() != null ? m.getOtorgadoPor().getId() : null)
                .nombreOtorgadoPor(m.getOtorgadoPor() != null ? m.getOtorgadoPor().getNombre() : null)
                .fechaOtorgamiento(m.getFechaOtorgamiento())
                .build();
    }

    public static List<InstitucionModuloResponseDTO> toResponseDTOList(List<InstitucionModulo> list) {
        return list.stream().map(InstitucionModuloMapper::toResponseDTO).toList();
    }
}
