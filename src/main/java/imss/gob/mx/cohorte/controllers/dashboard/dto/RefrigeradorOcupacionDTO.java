package imss.gob.mx.cohorte.controllers.dashboard.dto;

public record RefrigeradorOcupacionDTO(
    long   refrigeradorId,
    String nombre,
    long   totalPosiciones,  // suma de posiciones activas en todos los pisos del refrigerador
    long   ocupadas,         // posiciones con ocupada = true
    int    pct               // Math.round(ocupadas * 100.0 / totalPosiciones)
) {}
