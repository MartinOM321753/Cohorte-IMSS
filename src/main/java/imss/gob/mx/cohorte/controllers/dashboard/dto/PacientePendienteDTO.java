package imss.gob.mx.cohorte.controllers.dashboard.dto;

public record PacientePendienteDTO(
    String folio,
    String uuid,             // para abrir el expediente sin exponer el id en la URL
    String nombreCompleto,   // apellidoPaterno apellidoMaterno, nombre
    String sexo,             // "M" | "F"
    int    coberturaTotal,   // cuántos tipos tiene en total
    int    totalTipos        // denominador (N tipos activos)
) {}
