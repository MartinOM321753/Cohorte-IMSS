package imss.gob.mx.cohorte.controllers.analisis.dto;

import java.util.List;

/**
 * Resultado de la dispersión temporal: la lista ordenada de cortes de
 * seguimiento (eje X) y, por cada serie, sus puntos y su línea de tendencia.
 * Los cortes con menos de k participantes se ocultan (supresión k-anónima).
 */
public record DispersionTemporalDTO(
        String metricaClave,
        String metricaEtiqueta,
        String unidad,
        String grupo,
        List<String> cortes,
        List<SerieDispersionDTO> series,
        long n,
        long nSuprimido,
        boolean puntosIncluidos) {
}
