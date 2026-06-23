package imss.gob.mx.cohorte.controllers.institucion.dto;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.ParticipanteInstitucion;

import java.util.List;

public class ParticipanteInstitucionMapper {

    private ParticipanteInstitucionMapper() {}

    public static ParticipanteInstitucionResponseDTO toResponseDTO(ParticipanteInstitucion p) {
        if (p == null) return null;
        return ParticipanteInstitucionResponseDTO.builder()
                .id(p.getId())
                .idPaciente(p.getPaciente() != null ? p.getPaciente().getId() : null)
                .pacienteUuid(p.getPaciente() != null ? p.getPaciente().getUuid() : null)
                .institucion(mapInstitucion(p.getInstitucion()))
                .activo(p.getActivo())
                .observaciones(p.getObservaciones())
                .fechaAsignacion(p.getFechaAsignacion())
                .build();
    }

    public static List<ParticipanteInstitucionResponseDTO> toResponseDTOList(List<ParticipanteInstitucion> list) {
        return list.stream().map(ParticipanteInstitucionMapper::toResponseDTO).toList();
    }

    private static ParticipanteInstitucionResponseDTO.InstitucionResumenDTO mapInstitucion(Institucion i) {
        if (i == null) return null;
        return ParticipanteInstitucionResponseDTO.InstitucionResumenDTO.builder()
                .id(i.getId())
                .uuid(i.getUuid())
                .nombre(i.getNombre())
                .build();
    }
}
