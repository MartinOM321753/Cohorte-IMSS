package imss.gob.mx.cohorte.controllers.analisis.dto;

import java.util.List;

/**
 * Resultado agregado de una métrica: sus celdas (grupos) ya resumidas y
 * depuradas por supresión k-anónima, más la contabilidad de transparencia
 * (cuántos registros entraron y cuántos se ocultaron por celdas chicas).
 */
public record SerieAgregadaDTO(
        String metricaClave,
        String metricaEtiqueta,
        String grupo,
        String unidad,
        String tipo,          // NUMERICO | CATEGORICO
        String agregacion,    // RESUMEN | DISTRIBUCION | FUERA_DE_RANGO | FRECUENCIA
        List<String> dimensiones,
        List<CeldaAgregadaDTO> celdas,
        long nTotal,
        long nSuprimido,
        int  kAnonimo
) {}
