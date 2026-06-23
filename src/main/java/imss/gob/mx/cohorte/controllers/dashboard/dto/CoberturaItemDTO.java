package imss.gob.mx.cohorte.controllers.dashboard.dto;

public record CoberturaItemDTO(
    long   tipoId,           // id del Examen o TipoEstudio
    String nombre,           // nombre del tipo
    long   pacientesActivos, // denominador total
    long   conRegistro,      // pacientes con ≥ 1 resultado para este tipo
    long   enProceso,        // reservado; actualmente siempre 0
    long   sinRegistro,      // pacientesActivos - conRegistro
    int    pct               // Math.round(conRegistro * 100.0 / pacientesActivos)
) {}
