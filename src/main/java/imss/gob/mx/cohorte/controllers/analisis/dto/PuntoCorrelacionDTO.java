package imss.gob.mx.cohorte.controllers.analisis.dto;

/**
 * Un punto de la nube de correlación: el par (x, y) de un participante y el
 * grupo al que pertenece (para colorear). Solo se emite a usuarios con permiso
 * de export crudo, por ser dato a nivel participante.
 */
public record PuntoCorrelacionDTO(double x, double y, String grupo) {}
