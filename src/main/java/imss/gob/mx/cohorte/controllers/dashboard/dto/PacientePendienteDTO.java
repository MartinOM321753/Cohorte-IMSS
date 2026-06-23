package imss.gob.mx.cohorte.controllers.dashboard.dto;

public record PacientePendienteDTO(
    String folio,
    String nombreCompleto,   // apellidoPaterno apellidoMaterno, nombre
    String sexo,             // "M" | "F"
    int    coberturaTotal,   // cuántos tipos tiene en total
    int    totalTipos        // denominador (N tipos activos)
) {}
