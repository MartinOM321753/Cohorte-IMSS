package imss.gob.mx.cohorte.controllers.citas.dto;

import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.List;

public class CitaMapper {

    public static Cita toEntity(CitaRequestDTO dto) {
        Cita cita = new Cita();
        Paciente paciente = new Paciente();
        paciente.setUuid(dto.getPacienteUUID());
        cita.setPaciente(paciente);

        BeanUser usuario = new BeanUser();
        usuario.setUUID(dto.getUsuarioAgendaUUID());
        cita.setUsuarioAgenda(usuario);

        cita.setFechaCita(dto.getFechaCita());
        cita.setDuracionMinutos(dto.getDuracionMinutos());
        cita.setObservaciones(dto.getObservaciones());
        return cita;
    }

    public static CitaResponseDTO toResponseDTO(Cita c) {
        return CitaResponseDTO.builder()
            .id(c.getId())
            .estadoCita(c.getEstadoCita() != null ? c.getEstadoCita().name() : null)
            .fechaCita(c.getFechaCita())
            .duracionMinutos(c.getDuracionMinutos())
            .observaciones(c.getObservaciones())
            .fechaRegistro(c.getFechaRegistro() != null ? c.getFechaRegistro().toLocalDateTime() : null)
            .paciente(c.getPaciente() != null ? PacienteMapper.toResumenDTO(c.getPaciente()) : null)
            .usuarioAgenda(c.getUsuarioAgenda() != null ? UserMapper.toResumenDTO(c.getUsuarioAgenda()) : null)
            .build();
    }

    public static List<CitaResponseDTO> toResponseDTOList(List<Cita> list) {
        return list.stream().map(CitaMapper::toResponseDTO).toList();
    }
}
