package imss.gob.mx.cohorte.controllers.citas.dto;

import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.List;

public class CitaMapper {

    public static Cita toEntity(CitaRequestDTO dto) {
        if (dto.getTimezone() == null || dto.getTimezone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'timezone' es obligatorio");
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(dto.getTimezone());
        } catch (ZoneRulesException | DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Timezone inválida: '" + dto.getTimezone() + "'. Use un identificador IANA válido (ej. America/Mexico_City)");
        }

        LocalDateTime ldt;
        try {
            ldt = LocalDateTime.parse(dto.getStartAtLocal());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de fecha inválido: '" + dto.getStartAtLocal() + "'. Use el formato YYYY-MM-DDTHH:mm");
        }

        Instant startAtUtc = ZonedDateTime.of(ldt, zoneId).toInstant();
        int duration = dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 60;
        Instant endAtUtc = startAtUtc.plusSeconds(duration * 60L);

        Cita cita = new Cita();

        Paciente paciente = new Paciente();
        paciente.setUuid(dto.getPacienteUUID());
        cita.setPaciente(paciente);

        BeanUser usuario = new BeanUser();
        usuario.setUUID(dto.getUsuarioAgendaUUID());
        cita.setUsuarioAgenda(usuario);

        cita.setStartAtUtc(startAtUtc);
        cita.setEndAtUtc(endAtUtc);
        cita.setTimezone(dto.getTimezone());
        cita.setDurationMinutes(duration);
        cita.setColorHex(dto.getColorHex());
        cita.setObservaciones(dto.getObservaciones());
        return cita;
    }

    public static CitaResponseDTO toResponseDTO(Cita c) {
        return CitaResponseDTO.builder()
            .uuid(c.getUuid())
            .estadoCita(c.getEstadoCita() != null ? c.getEstadoCita().name() : null)
            .startAtUtc(c.getStartAtUtc())
            .endAtUtc(c.getEndAtUtc())
            .durationMinutes(c.getDurationMinutes())
            .timezone(c.getTimezone())
            .colorHex(c.getColorHex())
            .observaciones(c.getObservaciones())
            .createdAtUtc(c.getCreatedAtUtc())
            .paciente(c.getPaciente() != null ? PacienteMapper.toResumenDTO(c.getPaciente()) : null)
            .usuarioAgenda(c.getUsuarioAgenda() != null ? UserMapper.toResumenDTO(c.getUsuarioAgenda()) : null)
            .build();
    }

    public static List<CitaResponseDTO> toResponseDTOList(List<Cita> list) {
        return list.stream().map(CitaMapper::toResponseDTO).toList();
    }
}
