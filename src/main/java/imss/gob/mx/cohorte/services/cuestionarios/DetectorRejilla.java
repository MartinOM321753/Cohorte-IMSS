package imss.gob.mx.cohorte.services.cuestionarios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Descubre qué bloques del catálogo son, en el papel, una rejilla.
 *
 * <h3>El dato está escrito, solo que no en una columna</h3>
 *
 * <p>El catálogo no declara qué bloques son rejillas, pero lo lleva en los
 * enunciados: las ciento treinta y cinco celdas de la 112 se llaman
 * {@code Caminar Lunes}, {@code Caminar Martes}, {@code Caminar Tiempo}… La última
 * palabra es la columna y lo de antes es la fila. Pedir dos columnas más en el libro
 * sería más limpio, pero el libro ya existe y esto se deduce sin tocarlo.</p>
 *
 * <p>Hay una segunda forma de rotularlas, con guion: la tabla de la 57 —productos de
 * tabaco por lugar de compra— se llama {@code Cigarros-Gasolinera},
 * {@code Cigarros-Tienda de abarrotes}. Ahí la columna no es la última palabra, es
 * todo lo que sigue al guion. Se prueban las dos formas y gana la que arme la rejilla
 * más grande; si ninguna arma una que pase los umbrales, el bloque no es rejilla.</p>
 *
 * <h3>Por qué exige tanto antes de creérselo</h3>
 *
 * <p>Partir por el último espacio acierta en una rejilla y produce basura en
 * cualquier otro sitio, y una rejilla inventada es peor que ninguna: reacomoda en
 * filas y columnas preguntas que no tienen nada que ver, y el capturista deja de
 * encontrarlas donde las buscaba. Por eso no basta con que las partes existan:</p>
 *
 * <ul>
 *   <li>una columna solo cuenta si aparece en varias filas distintas —si no, es que
 *       dos enunciados terminaron con la misma palabra por casualidad—;</li>
 *   <li>una fila solo cuenta si tiene varias de esas columnas;</li>
 *   <li>y al final la rejilla tiene que estar casi llena. Un bloque con la mitad de
 *       los huecos vacíos no es una rejilla.</li>
 * </ul>
 *
 * <p>Con estos umbrales, en el catálogo real salen siete bloques y los siete son
 * rejillas de verdad: P112 (actividades × días), P131 (figura × edad y sexo), P193
 * (pregunta × trabajo), P57 (productos de tabaco × lugar de compra), P199 (bienes ×
 * de quién son), P203 (eventos de vida × tres preguntas) y P219 (anticonceptivo ×
 * tipo y meses).</p>
 *
 * <p>Dos bloques más son tablas en el papel y aquí no se reconocen, a propósito: la
 * 183 son <b>tres</b> tablas en un solo bloque —dónde, quiénes y si recibió atención,
 * por cada una de las siete situaciones— y sale una rejilla de 21 × 14 llena a un
 * tercio, que el umbral de densidad rechaza con razón; y la 182 no rotula la columna,
 * la repite («He peleado con él o ella» / «Que tan util fue he peleado con él o
 * ella»). Las dos se arreglan en el catálogo —partiendo el bloque, o rotulando la
 * columna— no aflojando el umbral: aflojarlo inventa rejillas en otros sitios.</p>
 *
 * <h3>Lo que queda fuera se queda fuera</h3>
 *
 * <p>Dentro de un bloque que sí es rejilla, lo que no encaja <b>no se fuerza</b>: el
 * «Ninguna» y el «Otro, especifique» de la 112 son preguntas del bloque pero no son
 * celdas de la tabla, así que se devuelven sin reparto y la pantalla las pinta
 * aparte.</p>
 */
public final class DetectorRejilla {

    private DetectorRejilla() {}

    private static final int MIN_FILAS = 3;
    private static final int MIN_COLUMNAS = 2;

    /** Una columna solo cuenta si se repite en al menos estas filas distintas. */
    private static final int MIN_FILAS_POR_COLUMNA = 3;

    /** Qué parte de la rejilla tiene que estar ocupada para creerse que es una rejilla. */
    private static final double MIN_DENSIDAD = 0.9;

    /**
     * Qué parte de lo que se pudo partir tiene que caber en la rejilla.
     *
     * <h3>Por qué hace falta, además de la densidad</h3>
     *
     * <p>La densidad mira si la tabla está llena; esto mira si la tabla <b>es</b> el
     * bloque. Un bloque donde la mitad de los enunciados se parten en algo que no
     * encaja no tiene una tabla con preguntas alrededor: tiene preguntas normales de
     * las que partir el enunciado saca basura, y dos o tres coinciden por casualidad
     * en la última palabra.</p>
     *
     * <p>El caso que lo obligó: las siete situaciones de violencia de la 183 —«Le han
     * agredido físicamente», «Le han agredido sexualmente», «Con qué frecuencia le han
     * agredido físicamente»…— arman por casualidad una rejilla de 4 × 2 llena del todo,
     * con «físicamente» y «sexualmente» de columnas. Son ocho de las catorce preguntas
     * que se pueden partir: 57%. La rejilla legítima más floja del catálogo es la 131,
     * con 26 de 34, o 76%. El umbral va en medio y con holgura de los dos lados.</p>
     */
    private static final double MIN_PARTIBLES_EN_REJILLA = 0.7;

    public record Celda(String fila, String columna) {}

    /**
     * @param enunciados el enunciado de cada pregunta del bloque, por su clave
     * @return dónde va cada pregunta que es una celda. <b>Mapa vacío si el bloque no
     *         es una rejilla</b>, que es el caso de casi todos.
     */
    public static <K> Map<K, Celda> repartir(Map<K, String> enunciados) {
        // Las dos formas de rotular una celda que trae el catálogo. Se prueban las dos
        // y gana la que arme la rejilla más grande; si ninguna arma una, no hay rejilla.
        Map<K, Celda> porGuion = repartirCon(enunciados, DetectorRejilla::partirPorGuion);
        Map<K, Celda> porEspacio = repartirCon(enunciados, DetectorRejilla::partirPorEspacio);
        return porGuion.size() >= porEspacio.size() ? porGuion : porEspacio;
    }

    private static <K> Map<K, Celda> repartirCon(Map<K, String> enunciados,
                                                 java.util.function.Function<String, Celda> partidor) {
        Map<K, Celda> partes = new LinkedHashMap<>();
        for (Map.Entry<K, String> e : enunciados.entrySet()) {
            Celda c = partidor.apply(e.getValue());
            if (c != null) partes.put(e.getKey(), c);
        }

        Map<String, Set<String>> filasPorColumna = new HashMap<>();
        for (Celda c : partes.values()) {
            filasPorColumna.computeIfAbsent(c.columna(), k -> new HashSet<>()).add(c.fila());
        }
        Set<String> columnas = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> c : filasPorColumna.entrySet()) {
            if (c.getValue().size() >= MIN_FILAS_POR_COLUMNA) columnas.add(c.getKey());
        }

        Map<String, Set<String>> columnasPorFila = new HashMap<>();
        for (Celda c : partes.values()) {
            if (columnas.contains(c.columna())) {
                columnasPorFila.computeIfAbsent(c.fila(), k -> new HashSet<>()).add(c.columna());
            }
        }
        Set<String> filas = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> f : columnasPorFila.entrySet()) {
            if (f.getValue().size() >= MIN_COLUMNAS) filas.add(f.getKey());
        }

        if (filas.size() < MIN_FILAS || columnas.size() < MIN_COLUMNAS) return Map.of();

        List<K> celdas = new ArrayList<>();
        for (Map.Entry<K, Celda> e : partes.entrySet()) {
            if (filas.contains(e.getValue().fila()) && columnas.contains(e.getValue().columna())) {
                celdas.add(e.getKey());
            }
        }
        if (celdas.size() < (double) filas.size() * columnas.size() * MIN_DENSIDAD) return Map.of();
        if (celdas.size() < partes.size() * MIN_PARTIBLES_EN_REJILLA) return Map.of();

        Map<K, Celda> reparto = new LinkedHashMap<>();
        for (K k : celdas) reparto.put(k, partes.get(k));
        return reparto;
    }

    /** {@code Caminar Lunes} → fila {@code Caminar}, columna {@code Lunes}. */
    private static Celda partirPorEspacio(String enunciado) {
        String t = limpiar(enunciado);
        if (t == null) return null;
        int corte = t.lastIndexOf(' ');
        if (corte <= 0 || corte == t.length() - 1) return null;
        return new Celda(t.substring(0, corte), t.substring(corte + 1));
    }

    /**
     * Los tres guiones con los que el catálogo separa fila y columna.
     *
     * <p>No es quisquillosería: la tabla de la 57 usa el guion corto
     * ({@code Cigarros-Gasolinera}) y las dos de la página 30 —bienes de la pareja y
     * eventos de vida— usan el largo ({@code Coche – Usted posee}). Con solo el corto,
     * esas dos seguían pintándose como una lista de 28 y 45 campos sueltos.</p>
     */
    private static final String GUIONES = "-–—";

    /**
     * {@code Cigarros-Gasolinera} → fila {@code Cigarros}, columna {@code Gasolinera}.
     *
     * <p>La otra forma de rotular una celda que usa el catálogo, y la que tiene la
     * tabla de la 57: seis productos de tabaco por nueve lugares de compra. Ahí la
     * columna no es la última palabra —«Tienda de abarrotes», «Yo no compré este
     * producto»— sino todo lo que sigue al guion.</p>
     *
     * <p>Se parte por el <b>primer</b> guion y no por el último porque el que separa
     * las dos mitades es ese: un guion posterior pertenece al nombre de la columna
     * —«Casa / departamento – Poseen juntos» no tiene más, pero «Se incrementaron sus
     * dificultades para caminar – ¿Por qué período las suspendió?» sí—.</p>
     */
    private static Celda partirPorGuion(String enunciado) {
        String t = limpiar(enunciado);
        if (t == null) return null;
        int corte = -1;
        for (int i = 0; i < t.length(); i++) {
            if (GUIONES.indexOf(t.charAt(i)) >= 0) {
                corte = i;
                break;
            }
        }
        if (corte <= 0 || corte == t.length() - 1) return null;
        String fila = t.substring(0, corte).trim();
        String columna = t.substring(corte + 1).trim();
        if (fila.isEmpty() || columna.isEmpty()) return null;
        return new Celda(fila, columna);
    }

    private static String limpiar(String enunciado) {
        if (enunciado == null) return null;
        return enunciado.replaceAll("[\\s\\u00A0]+", " ").trim();
    }
}
