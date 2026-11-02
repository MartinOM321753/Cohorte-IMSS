package imss.gob.mx.cohorte.controllers.analisis.dto;

import java.util.List;
import java.util.Map;

/**
 * Una celda del resultado agregado: un grupo definido por sus valores de
 * dimensión (sexo, rango de edad, institución) con sus estadísticos. Solo se
 * emite si pasa la supresión k-anónima.
 *
 * <p>Los campos no aplicables a la agregación pedida vienen en {@code null} o
 * vacíos (p. ej. {@code histograma} solo en DISTRIBUCION; {@code categorias}
 * solo en FRECUENCIA).</p>
 */
public record CeldaAgregadaDTO(
        Map<String, String> dimensiones,
        long                n,

        // ── Resumen numérico ──
        Double media,
        Double mediana,
        Double p25,
        Double p75,
        Double min,
        Double max,
        Double desv,

        // ── Fuera de rango (según sexo del participante) ──
        Long enRango,
        Long fueraDeRango,
        Long sinReferencia,

        // ── Distribución ──
        List<BinHistogramaDTO> histograma,

        // ── Frecuencia (categóricas) ──
        List<CategoriaConteoDTO> categorias
) {}
