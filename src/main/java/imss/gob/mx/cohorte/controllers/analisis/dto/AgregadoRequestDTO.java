package imss.gob.mx.cohorte.controllers.analisis.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Petición de agregación del módulo de análisis.
 *
 * <p>El cliente pide una o varias métricas, cómo desglosarlas ({@code dimensiones})
 * y qué recortar ({@code filtros}). El servidor resuelve el alcance de
 * instituciones y nunca agrega fuera de él, aunque el filtro pida más.</p>
 */
@Data
public class AgregadoRequestDTO {

    /** Claves de métrica a agregar (p. ej. {@code estudio.param.85}). */
    @NotEmpty(message = "Hay que elegir al menos una métrica")
    private List<String> metricas = new ArrayList<>();

    /** Dimensiones de desglose, en orden: {@code institucion}, {@code sexo}, {@code rango_edad}. Vacío = total. */
    private List<String> dimensiones = new ArrayList<>();

    /** {@code RESUMEN} | {@code DISTRIBUCION} | {@code FUERA_DE_RANGO} | {@code FRECUENCIA} | {@code TEMPORAL} | {@code MAPA_CALOR}. */
    private String agregacion = "RESUMEN";

    /**
     * Granularidad del eje temporal cuando {@code agregacion = TEMPORAL}:
     * {@code MES} | {@code TRIMESTRE} | {@code ANIO}. El servidor deriva un
     * "corte de seguimiento" de la fecha de cada medición.
     */
    private String granularidad = "MES";

    private Filtros filtros = new Filtros();

    @Data
    public static class Filtros {
        /** Subconjunto de instituciones del alcance; vacío = todo el alcance. */
        private Set<Long> instituciones;

        /** {@code "M"} / {@code "F"}; vacío = ambos (y sin dato). */
        private List<String> sexos;

        /** Edad mínima/máxima (a la fecha de la medición), inclusivas. */
        private Integer edadMin;
        private Integer edadMax;

        /**
         * Cortes de edad definidos por el analista (R3): lista de pares
         * {@code [min, max]} inclusivos, p. ej. {@code [[18,29],[30,39]]}. Solo
         * aplica si la dimensión {@code rango_edad} está activa.
         */
        private List<List<Integer>> rangosEdad;

        /** Rango de fechas de la medición, en ISO {@code yyyy-MM-dd}. */
        private String fechaDesde;
        private String fechaHasta;

        /** Recorte por valor de la métrica (solo numéricas). */
        private Double valorMin;
        private Double valorMax;

        /** Número de barras del histograma (solo {@code DISTRIBUCION}). */
        private Integer bins;
    }
}
