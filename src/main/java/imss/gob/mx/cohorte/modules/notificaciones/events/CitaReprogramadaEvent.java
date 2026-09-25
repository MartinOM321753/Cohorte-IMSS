package imss.gob.mx.cohorte.modules.notificaciones.events;

import imss.gob.mx.cohorte.modules.cita.Cita;

import java.time.Instant;

/** Publicado cuando se modifica la fecha u hora de una cita existente. */
public record CitaReprogramadaEvent(Cita cita, Instant fechaAnteriorUtc) {}
