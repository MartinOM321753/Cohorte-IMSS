package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import java.util.List;

/**
 * Consultas del listado de muestras que se arman en tiempo de ejecución.
 *
 * <p>Son un fragmento aparte de {@link MuestraRepository} porque la pantalla
 * combina nueve criterios opcionales con una paginación por llave: escribirlo
 * como una consulta fija obligaría a la forma {@code (:param IS NULL OR ...)}
 * repetida nueve veces, que impide a la base descartar índices y obliga a
 * enviar parámetros que nadie usa. Aquí cada cláusula se agrega solo si el
 * criterio viene, y solo entonces se enlaza su parámetro.</p>
 */
public interface MuestraCursorRepository {

    /**
     * Una página de tarjetas del listado, en orden de la más reciente a la más
     * antigua.
     *
     * <p>Devuelve solo filas «de primer nivel»: muestras sin padre, más las
     * alícuotas cuyo padre no es visible para esta institución —recibidas en
     * préstamo, con el tubo original en otra sede—, que en la pantalla se
     * dibujan como tarjeta propia.</p>
     *
     * @param cursor    frontera de la que se parte; null = desde el extremo
     * @param haciaAtras false avanza hacia lo más antiguo, true hacia lo más reciente
     * @param limite    cuántas filas traer; conviene pedir una de más para saber
     *                  si queda página siguiente sin contar toda la tabla
     */
    List<Muestra> buscarPagina(CriteriosMuestra criterios, CursorMuestra cursor,
                               boolean haciaAtras, int limite);

    /** Total de tarjetas que cumplen los criterios, para el «N de M» de la pantalla. */
    long contarPagina(CriteriosMuestra criterios);

    /**
     * Alícuotas visibles de las padres dadas, en una sola consulta.
     *
     * <p>Se piden en bloque y no una por tarjeta: veinte tarjetas serían veinte
     * viajes a la base para pintar una pantalla.</p>
     */
    List<Muestra> buscarAlicuotasDe(List<Long> idsPadre, CriteriosMuestra criterios);

    /**
     * Cuántas alícuotas huérfanas devueltas quedarían ocultas.
     *
     * <p>Alimenta el botón que las revela. Se cuenta aparte porque la vista
     * normal las esconde, y sin el número el botón no podría decir cuántas hay
     * detrás.</p>
     */
    long contarHuerfanasDevueltas(CriteriosMuestra criterios);
}
