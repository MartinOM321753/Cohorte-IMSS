package imss.gob.mx.cohorte.controllers.citas.dto;

import imss.gob.mx.cohorte.modules.cita.ConfiguracionHorario;

import java.util.List;

public class ConfiguracionHorarioMapper {

    private ConfiguracionHorarioMapper() {}

    public static ConfiguracionHorarioResponseDTO toResponseDTO(ConfiguracionHorario entity) {
        return ConfiguracionHorarioResponseDTO.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .horaInicio(entity.getHoraInicio())
                .horaFin(entity.getHoraFin())
                .lunes(entity.getLunes())
                .martes(entity.getMartes())
                .miercoles(entity.getMiercoles())
                .jueves(entity.getJueves())
                .viernes(entity.getViernes())
                .sabado(entity.getSabado())
                .domingo(entity.getDomingo())
                .activa(entity.getActiva())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }

    public static List<ConfiguracionHorarioResponseDTO> toResponseDTOList(List<ConfiguracionHorario> entities) {
        return entities.stream().map(ConfiguracionHorarioMapper::toResponseDTO).toList();
    }

    public static ConfiguracionHorario toEntity(ConfiguracionHorarioRequestDTO dto) {
        ConfiguracionHorario entity = new ConfiguracionHorario();
        entity.setNombre(dto.getNombre());
        entity.setHoraInicio(dto.getHoraInicio());
        entity.setHoraFin(dto.getHoraFin());
        entity.setLunes(dto.getLunes() != null ? dto.getLunes() : true);
        entity.setMartes(dto.getMartes() != null ? dto.getMartes() : true);
        entity.setMiercoles(dto.getMiercoles() != null ? dto.getMiercoles() : true);
        entity.setJueves(dto.getJueves() != null ? dto.getJueves() : true);
        entity.setViernes(dto.getViernes() != null ? dto.getViernes() : true);
        entity.setSabado(dto.getSabado() != null ? dto.getSabado() : false);
        entity.setDomingo(dto.getDomingo() != null ? dto.getDomingo() : false);
        entity.setActiva(dto.getActiva() != null ? dto.getActiva() : false);
        return entity;
    }
}
