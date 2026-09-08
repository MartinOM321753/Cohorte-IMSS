package imss.gob.mx.cohorte.services.reportes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Qué columnas puede llevar cada tabla, y cómo se llaman.
 *
 * <p>Existe porque cada bloque imprime cosas distintas y el panel de propiedades
 * ofrecía siempre las mismas: en el listado de estudios se podían marcar
 * «Parámetro / Resultado / Unidad / Referencia» mientras el documento salía con
 * «Estudio / Fecha / Resultados». Las casillas no correspondían a nada.</p>
 *
 * <p>La lista vive aquí y viaja al editor dentro del catálogo, de modo que lo que
 * se ofrece marcar y lo que se sabe imprimir salen del mismo sitio.</p>
 */
public final class ColumnasBloque {

    private ColumnasBloque() {}

    /** Columnas de la tabla de resultados de un estudio. */
    public static final Map<String, String> RESULTADOS = columnas(
            "parametro", "Parámetro",
            "valor", "Resultado",
            "unidad", "Unidad",
            "referencia", "Referencia");

    /** Columnas del listado de estudios del participante. */
    public static final Map<String, String> LISTADO_ESTUDIOS = columnas(
            "estudio", "Estudio",
            "fecha", "Fecha",
            "resultados", "Resultados");

    /** Columnas de la tabla de resultados de laboratorio. */
    public static final Map<String, String> LISTADO_EXAMENES = columnas(
            "examen", "Examen",
            "valor", "Resultado",
            "unidad", "Unidad",
            "referencia", "Referencia",
            "fecha", "Fecha");

    /** Las que admite ese bloque, o vacío si el bloque no lleva tabla. */
    public static Map<String, String> de(String clave) {
        if (clave == null) return Map.of();
        if (ClaveCampo.BLOQUE_LISTADO_ESTUDIOS.equals(clave)) return LISTADO_ESTUDIOS;
        if (ClaveCampo.BLOQUE_LISTADO_EXAMENES.equals(clave)) return LISTADO_EXAMENES;

        ClaveCampo.BloqueEstudio bloque = ClaveCampo.comoBloqueEstudio(clave);
        if (bloque != null && "resultados".equals(bloque.bloque())) return RESULTADOS;

        // Las evidencias no son una tabla: no tienen columnas que elegir.
        return Map.of();
    }

    public static List<String> clavesDe(String clave) {
        return List.copyOf(de(clave).keySet());
    }

    /**
     * El orden importa: es el orden en que salen las columnas en el documento y en
     * que se ofrecen en el panel. Por eso se envuelve un LinkedHashMap y no se usa
     * Map.copyOf, que no garantiza ninguno.
     */
    private static Map<String, String> columnas(String... pares) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) mapa.put(pares[i], pares[i + 1]);
        return java.util.Collections.unmodifiableMap(mapa);
    }
}
