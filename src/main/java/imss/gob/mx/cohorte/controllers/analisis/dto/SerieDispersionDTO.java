package imss.gob.mx.cohorte.controllers.analisis.dto;

import java.util.List;

/**
 * Una serie de la dispersión temporal (un grupo del desglose, o «Todos» si no se
 * desglosa): sus puntos individuales (solo con permiso de crudo) y su línea de
 * tendencia (media por corte, siempre).
 */
public record SerieDispersionDTO(
        String grupo,
        List<PuntoDispersionDTO> puntos,
        List<PuntoTendenciaDTO> tendencia) {
}
