package imss.gob.mx.cohorte.controllers.dashboard.dto;

public record CeldaCoberturaDTO(
    long   tipoId,
    String estado,  // "HECHO" | "PROCESO" | "FALTA"
    Long   refId    // id del estudio/resultado más reciente de ese tipo; null si FALTA
) {}
