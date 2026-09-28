package imss.gob.mx.cohorte.controllers.citas.dto;

import java.time.Instant;

/**
 * Proyección MÍNIMA de una cita para pintar el calendario y su modal: solo lo que
 * se muestra (nombre del participante, nombre de quien agenda, horario, estado,
 * color y observaciones). Evita traer folio, sexo, institución, usuario completo,
 * etc. que el calendario no usa. La lista de citas usa este DTO, no el completo.
 */
public record CitaCalendarDTO(
    String  uuid,
    String  estadoCita,
    Instant startAtUtc,
    Integer durationMinutes,
    String  colorHex,
    String  observaciones,
    Ref     paciente,
    Ref     usuarioAgenda
) {
    /** Referencia mínima: solo lo que el calendario/modal necesitan. */
    public record Ref(String uuid, String nombreCompleto) {}
}
