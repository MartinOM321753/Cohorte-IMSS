package imss.gob.mx.cohorte.services.citas;


import imss.gob.mx.cohorte.controllers.citas.dto.CitaPatchDTO;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.cita.EstadoCita;
import imss.gob.mx.cohorte.modules.cita.CitaRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.Arrays;
import java.util.List;

@Service
@AllArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final InstitucionContextService institucionContextService;

    public List<Cita> getAll() {
        return citaRepository.findAllByInstitucion_Id(institucionContextService.getIdInstitucionActual());
    }

    public List<Cita> getByRange(Instant start, Instant end) {
        return citaRepository.findByStartAtUtcBetweenAndInstitucion_Id(start, end, institucionContextService.getIdInstitucionActual());
    }

    public Cita getByUuid(String uuid) {
        return citaRepository.findByUuidAndInstitucion_Id(uuid, institucionContextService.getIdInstitucionActual())
                .orElseThrow(() -> new ObjNotFoundException("La cita no existe con UUID: " + uuid));
    }

    public Cita findPatientFolio(String folio){
        Cita cita = citaRepository.findByPaciente_Folio(folio)
                .orElseThrow(() -> new ObjNotFoundException("El folio no cuenta con una cita asignada"));
        institucionContextService.verificarPertenece(cita.getInstitucion());
        return cita;
    }

    public Cita findPatientUuid(String uuid){
        Cita cita = citaRepository.findByPaciente_Uuid(uuid)
                .orElseThrow(() -> new ObjNotFoundException("El participante no cuenta con una cita asignada"));
        institucionContextService.verificarPertenece(cita.getInstitucion());
        return cita;
    }

    public List<Cita> findAllByPacienteUuid(String uuid) {
        return citaRepository.findAllByPaciente_UuidAndInstitucion_IdOrderByStartAtUtcDesc(
                uuid, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public Cita create(Cita cita) {
        validateFutureDate(cita.getStartAtUtc());
        validateNoCollision(cita);
        cita.setEstadoCita(EstadoCita.Programada);
        cita.setInstitucion(institucionContextService.getInstitucionActual());
        return citaRepository.save(cita);
    }

    @Transactional
    public Cita patch(String uuid, CitaPatchDTO patch) {
        Cita cita = getByUuid(uuid);
        Instant now = Instant.now();

        // Calcular endAtUtc en memoria para citas legacy donde el campo puede ser null
        Instant currentEnd = cita.getEndAtUtc() != null
                ? cita.getEndAtUtc()
                : cita.getStartAtUtc().plusSeconds(
                        (cita.getDurationMinutes() != null ? cita.getDurationMinutes() : 60) * 60L);

        boolean isPast = currentEnd.isBefore(now);

        // Bloquear cualquier cambio de horario/duración/timezone en citas que ya finalizaron
        if (patch.getStartAtLocal() != null || patch.getTimezone() != null || patch.getDurationMinutes() != null) {
            if (isPast) {
                throw new ObjConflictException(
                        "La cita ya finalizó. Solo se permite modificar estado, color u observaciones.");
            }

            String tz = patch.getTimezone() != null ? patch.getTimezone() : cita.getTimezone();
            Instant newStart = cita.getStartAtUtc();

            if (patch.getStartAtLocal() != null) {
                ZoneId zoneId;
                try {
                    zoneId = ZoneId.of(tz);
                } catch (ZoneRulesException | DateTimeParseException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Timezone inválida: '" + tz + "'");
                }
                try {
                    newStart = ZonedDateTime.of(LocalDateTime.parse(patch.getStartAtLocal()), zoneId).toInstant();
                } catch (DateTimeParseException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Formato de fecha inválido: '" + patch.getStartAtLocal() + "'. Use YYYY-MM-DDTHH:mm");
                }
                cita.setTimezone(tz);
            }

            int newDuration = patch.getDurationMinutes() != null
                    ? patch.getDurationMinutes()
                    : (cita.getDurationMinutes() != null ? cita.getDurationMinutes() : 60);

            // Calcular endAtUtc explícitamente antes de validar colisiones
            Instant newEnd = newStart.plusSeconds(newDuration * 60L);

            validateFutureDate(newStart);

            cita.setStartAtUtc(newStart);
            cita.setEndAtUtc(newEnd);
            cita.setDurationMinutes(newDuration);

            validateNoCollision(cita);
        }

        if (patch.getColorHex() != null) cita.setColorHex(patch.getColorHex());
        if (patch.getObservaciones() != null) cita.setObservaciones(patch.getObservaciones());
        if (patch.getEstadoCita() != null) {
            cita.setEstadoCita(parseEstadoCita(patch.getEstadoCita()));
        }

        return citaRepository.save(cita);
    }

    private void validateFutureDate(Instant start) {
        // Estricto: startAtUtc debe ser estrictamente mayor que ahora (no igual)
        if (!start.isAfter(Instant.now())) {
            throw new ObjConflictException("La fecha de inicio debe ser estrictamente futura");
        }
    }

    private void validateNoCollision(Cita cita) {
        Instant start = cita.getStartAtUtc();
        Instant end = cita.getEndAtUtc() != null
                ? cita.getEndAtUtc()
                : start.plusSeconds(cita.getDurationMinutes() * 60L);

        List<Cita> collisions = citaRepository.findOverlappingCitas(
                cita.getUsuarioAgenda().getUUID(),
                start,
                end,
                cita.getUuid()
        );

        if (!collisions.isEmpty()) {
            throw new ObjConflictException("El horario seleccionado colisiona con otra cita existente para este usuario");
        }
    }

    private EstadoCita parseEstadoCita(String raw) {
        String normalized = raw.trim().replace(" ", "_");
        for (EstadoCita estado : EstadoCita.values()) {
            if (estado.name().equalsIgnoreCase(normalized)) {
                return estado;
            }
        }
        String validos = Arrays.stream(EstadoCita.values())
                .map(EstadoCita::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Estado inválido: '" + raw + "'. Valores aceptados: " + validos);
    }

    @Transactional
    public void cancelar(String uuid) {
        Cita cita = getByUuid(uuid);
        cita.setEstadoCita(EstadoCita.Cancelada);
        citaRepository.save(cita);
    }
}