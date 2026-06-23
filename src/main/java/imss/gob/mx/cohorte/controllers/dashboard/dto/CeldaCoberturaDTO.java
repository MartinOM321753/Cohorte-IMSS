package imss.gob.mx.cohorte.controllers.dashboard.dto;

public record CeldaCoberturaDTO(
    long   tipoId,
    String estado   // "HECHO" | "PROCESO" | "FALTA"
) {}
