package imss.gob.mx.cohorte.modules.analisis;

/**
 * Unidad analizable común a las cuatro fuentes de resultados. Es la capa
 * semántica que permite que "ver por estudio por separado" y "cruzar entre
 * estudios" sean la misma máquina con distinto filtro.
 *
 * <p>No se persiste: el catálogo se construye en tiempo de ejecución a partir de
 * los parámetros/exámenes activos del alcance del usuario
 * ({@code CatalogoMetricasService}). La {@code clave} es el identificador estable
 * con el que el cliente pide agregaciones y exportaciones.</p>
 *
 * @param clave         identificador estable, p. ej. {@code estudio.param.85},
 *                      {@code examen.12}, {@code somato.imc}
 * @param fuente        de qué circuito proviene
 * @param etiqueta      nombre legible de la variable ("Glucosa en ayuno")
 * @param grupo         estudio/examen al que pertenece, para agrupar en la UI
 *                      ("Química sanguínea", "Somatometría")
 * @param unidad        unidad de medida, o {@code null}
 * @param tipo          numérica o categórica (define la agregación posible)
 * @param refMinHombres límite inferior de referencia para hombres, o {@code null}
 * @param refMaxHombres límite superior de referencia para hombres, o {@code null}
 * @param refMinMujeres límite inferior de referencia para mujeres, o {@code null}
 * @param refMaxMujeres límite superior de referencia para mujeres, o {@code null}
 * @param origenId      id del parámetro/examen de origen en su propia tabla
 * @param institucionId institución dueña del catálogo del que sale la métrica
 */
public record Metrica(
        String        clave,
        FuenteMetrica fuente,
        String        etiqueta,
        String        grupo,
        String        unidad,
        TipoMetrica   tipo,
        Double        refMinHombres,
        Double        refMaxHombres,
        Double        refMinMujeres,
        Double        refMaxMujeres,
        Long          origenId,
        Long          institucionId
) {
    /** ¿Tiene algún rango de referencia definido (para poder calcular "fuera de rango")? */
    public boolean tieneReferencia() {
        return refMinHombres != null || refMaxHombres != null
            || refMinMujeres != null || refMaxMujeres != null;
    }
}
