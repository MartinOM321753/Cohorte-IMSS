package imss.gob.mx.cohorte.modules.notificaciones.events;

import imss.gob.mx.cohorte.modules.cita.Cita;

/** Publicado cuando una cita es creada exitosamente. */
public record CitaAgendadaEvent(Cita cita) {}
