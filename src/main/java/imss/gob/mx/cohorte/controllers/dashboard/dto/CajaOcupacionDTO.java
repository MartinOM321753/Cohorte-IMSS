package imss.gob.mx.cohorte.controllers.dashboard.dto;

/**
 * Proyección de ocupación de una caja criogénica para el dashboard.
 */
public record CajaOcupacionDTO(
        Long   cajaId,
        String codigo,
        String tipoCaja,
        long   totalPosiciones,
        long   ocupadas,
        int    pct
) {}
