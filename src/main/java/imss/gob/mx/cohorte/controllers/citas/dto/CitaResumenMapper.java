package imss.gob.mx.cohorte.controllers.citas.dto;

import imss.gob.mx.cohorte.modules.cita.Cita;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public final class CitaResumenMapper {

    private CitaResumenMapper() {}

    public static CitaResumenDTO toResumenDTO(Cita cita) {
        LocalDateTime fecha = null;
        if (cita.getStartAtUtc() != null) {
            String tz = (cita.getTimezone() != null && !cita.getTimezone().isBlank())
                    ? cita.getTimezone()
                    : "UTC";
            try {
                fecha = LocalDateTime.ofInstant(cita.getStartAtUtc(), ZoneId.of(tz));
            } catch (Exception e) {
                fecha = LocalDateTime.ofInstant(cita.getStartAtUtc(), ZoneId.of("UTC"));
            }
        }

        String profesional = null;
        if (cita.getUsuarioAgenda() != null && cita.getUsuarioAgenda().getPersona() != null) {
            var p = cita.getUsuarioAgenda().getPersona();
            profesional = (p.getNombre() + " " + p.getApellidoPaterno()
                    + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "")).trim();
        }

        return CitaResumenDTO.builder()
                .citaUuid(cita.getUuid())
                .pacienteUuid(cita.getPaciente() != null ? cita.getPaciente().getUuid() : null)
                .fecha(fecha)
                .tipo(cita.getObservaciones())
                .estado(cita.getEstadoCita() != null ? cita.getEstadoCita().name() : null)
                .profesional(profesional)
                .build();
    }

    public static List<CitaResumenDTO> toResumenDTOList(List<Cita> citas) {
        return citas.stream().map(CitaResumenMapper::toResumenDTO).toList();
    }
}
