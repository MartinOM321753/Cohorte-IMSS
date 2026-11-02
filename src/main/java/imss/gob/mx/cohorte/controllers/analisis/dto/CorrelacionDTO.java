package imss.gob.mx.cohorte.controllers.analisis.dto;

import java.util.List;

/**
 * Resultado de una correlación entre dos métricas numéricas.
 *
 * <p>Los estadísticos de ajuste (pendiente, intercepto, R²) y las medias son
 * agregados y se emiten siempre que haya al menos {@code k} pares. Los
 * {@code puntos} individuales son dato a nivel participante: solo se incluyen si
 * el usuario tiene el permiso {@code ANALISIS_EXPORTA_CRUDO}
 * ({@code puntosIncluidos}).</p>
 */
public record CorrelacionDTO(
        String metricaXClave,
        String metricaXEtiqueta,
        String unidadX,
        String metricaYClave,
        String metricaYEtiqueta,
        String unidadY,
        String grupo,
        long   n,
        long   nSuprimido,
        Double pendiente,
        Double intercepto,
        Double r2,
        Double mediaX,
        Double mediaY,
        Double minX,
        Double maxX,
        boolean puntosIncluidos,
        List<PuntoCorrelacionDTO> puntos
) {}
