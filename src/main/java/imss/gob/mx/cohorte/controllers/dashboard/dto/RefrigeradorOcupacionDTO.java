package imss.gob.mx.cohorte.controllers.dashboard.dto;

import java.util.List;

public record RefrigeradorOcupacionDTO(
    long             refrigeradorId,
    String           nombre,
    long             totalPosiciones,
    long             ocupadas,
    int              pct,
    List<PisoResumen> pisos
) {
    /** Ocupación por piso individual. */
    public record PisoResumen(
        Long   pisoId,
        String numeroPiso,
        long   total,
        long   ocupadas,
        int    pct
    ) {}
}
