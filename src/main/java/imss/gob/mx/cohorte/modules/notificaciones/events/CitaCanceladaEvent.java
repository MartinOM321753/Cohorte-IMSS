package imss.gob.mx.cohorte.modules.notificaciones.events;

import imss.gob.mx.cohorte.modules.cita.Cita;

/** Publicado cuando una cita es cancelada. */
public record CitaCanceladaEvent(Cita cita) {}
