package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Las columnas que la carga masiva de muestras entiende.
 *
 * <p>A diferencia de la carga de estudios, aquí el conjunto de columnas es
 * <b>cerrado</b>: no depende de un catálogo configurable, sino de los campos que
 * una muestra tiene. Por eso vive en un enum y no se resuelve contra alias
 * guardados en la base.</p>
 *
 * <p>Cada columna admite varios títulos porque el archivo lo escribe una persona
 * y no un instrumento: {@code numeroAlicuota}, {@code No. alícuota} y
 * {@code MUESTRA} —como lo titulaba la hoja original del cliente— son la misma
 * columna. La comparación se hace sobre la forma normalizada
 * ({@link NormalizadorAlias}) y además sin puntuación, para no tener que
 * enumerar cada variante de punto y acento.</p>
 */
public enum ColumnaMuestra {

    /** A quién pertenece el vial. Es lo único que identifica al participante. */
    FOLIO("folio", true, 14,
            "Obligatoria. El folio del participante, de 6 dígitos y guardado como texto.",
            "FOLIO", "NO FOLIO", "NUM FOLIO", "NUMERO DE FOLIO",
            "FOLIO PARTICIPANTE", "FOLIO DEL PARTICIPANTE"),

    /** Nombre del tipo de muestra en el catálogo de la institución. */
    TIPO_MUESTRA("tipoMuestra", true, 16,
            "Obligatoria. El nombre exacto del tipo en el catálogo de su institución.",
            "TIPOMUESTRA", "TIPO MUESTRA", "TIPO DE MUESTRA", "TIPO"),

    /** Nombre del tubo dentro de ese tipo. Define el cupo y el volumen nominal. */
    TUBO("tubo", true, 20,
            "Obligatoria. El nombre exacto del tubo, que tiene que pertenecer a ese tipo.",
            "TUBO", "TUBOMUESTRA", "TUBO MUESTRA", "TUBO DE MUESTRA",
            "RECIPIENTE", "CONTENEDOR"),

    /** Número del vial dentro de su lote, 1-based. */
    NUMERO_ALICUOTA("numeroAlicuota", true, 16,
            "Obligatoria. De 1 en adelante, sin pasar del número de alícuotas del tubo.",
            "NUMEROALICUOTA", "NUMERO ALICUOTA", "NUMERO DE ALICUOTA",
            "NO ALICUOTA", "ALICUOTA", "MUESTRA", "VIAL"),

    /**
     * Fecha de toma. No es obligatoria: si falta la columna entera, la pantalla
     * pide una fecha para todo el archivo.
     */
    FECHA("fecha", false, 14,
            "Fecha de toma. Si se deja vacía se usa la que elija en la pantalla.",
            "FECHA", "FECHA DE TOMA", "FECHA TOMA", "FECHA DE RECOLECCION",
            "FECHA RECOLECCION", "FECHA DE MUESTRA", "FECHA MUESTRA",
            "FECHA DE ENTREGA", "FECHA Y HORA"),

    /** Lo que de verdad salió en ese vial. Vacío hereda el nominal del tubo. */
    VOLUMEN("volumen", false, 11,
            "Lo que salió en ese vial. Vacío hereda el volumen configurado del tubo.",
            "VOLUMEN", "VALOR", "CANTIDAD"),

    /** Unidad del volumen. Vacía hereda la del tubo; distinta es error. */
    UNIDAD("unidad", false, 10,
            "Vacía hereda la del tubo. Si no coincide con ella, la fila es un error: "
            + "no se convierten unidades.",
            "UNIDAD", "UNIDAD DE VOLUMEN", "UNIDAD VOLUMEN"),

    /** Código de la caja criogénica tal como está dada de alta. */
    CODIGO_CAJA("codigoCaja", false, 14,
            "El código de la caja tal como está dada de alta. Vacío deja el vial sin ubicar.",
            "CODIGOCAJA", "CODIGO CAJA", "CODIGO DE CAJA", "CAJA"),

    /** Hueco dentro de la caja, en la forma A1 / B7 / AA12. */
    POSICION("posicion", false, 11,
            "El hueco dentro de la caja: la fila en letra y la columna en número, como A1 o B7.",
            "POSICION", "UBICACION", "POSICION EN CAJA", "POSICION CAJA"),

    /** Texto libre. Aquí sí es una observación de verdad. */
    OBSERVACIONES("observaciones", false, 40,
            "Texto libre, máximo 200 caracteres.",
            "OBSERVACIONES", "OBSERVACION", "NOTAS", "COMENTARIOS");

    private final String encabezado;
    private final boolean obligatoria;
    private final int anchoPlantilla;
    private final String descripcion;
    private final List<String> titulos;

    ColumnaMuestra(String encabezado, boolean obligatoria, int anchoPlantilla,
                   String descripcion, String... titulos) {
        this.encabezado = encabezado;
        this.obligatoria = obligatoria;
        this.anchoPlantilla = anchoPlantilla;
        this.descripcion = descripcion;
        this.titulos = List.of(titulos);
    }

    public boolean esObligatoria() {
        return obligatoria;
    }

    /**
     * El título que escribe la plantilla, y el que se cita en los mensajes.
     *
     * <p>Vive aquí y no en quien genera la plantilla para que no puedan
     * separarse: si alguien añade una columna al enum, la plantilla la incluye
     * sola en lugar de quedarse atrás en silencio.</p>
     */
    public String tituloPreferido() {
        return encabezado;
    }

    /** Qué significa la columna, para la hoja de instrucciones de la plantilla. */
    public String descripcion() {
        return descripcion;
    }

    /** Ancho con el que la plantilla dibuja la columna, en caracteres. */
    public int anchoPlantilla() {
        return anchoPlantilla;
    }

    /**
     * Títulos que se ignoran sin avisar.
     *
     * <p>La plantilla los lleva a propósito para que una persona pueda revisar la
     * hoja —el nombre del participante y la caja de origen son la única forma de
     * leerla sin descifrar folios—, pero el importador no los usa. Avisar de
     * ellos como «columna desconocida» sería avisar de nuestras propias columnas,
     * y el usuario aprendería a ignorar los avisos.</p>
     */
    private static final Set<String> INFORMATIVAS = Set.of(
            "NOMBRE", "NOMBRE DEL PARTICIPANTE", "PARTICIPANTE",
            "CONSECUTIVO", "NO CONSECUTIVO", "CAJAORIGEN", "CAJA ORIGEN");

    /**
     * Las que la plantilla escribe para que una persona pueda leer la hoja, y el
     * importador descarta sin decir nada.
     *
     * <p>Sale de la misma lista que usa el emparejado, así que una columna que la
     * plantilla incluya no puede acabar avisada como desconocida.</p>
     */
    public static List<String> encabezadosInformativos() {
        return List.of("nombre", "consecutivo");
    }

    /**
     * Una columna del archivo ya interpretada.
     *
     * @param campo null cuando el encabezado no corresponde a ninguna columna
     *              conocida ni a una informativa
     */
    public record Columna(int indice, String encabezado, ColumnaMuestra campo, boolean informativa) {}

    /**
     * El resultado del emparejado.
     *
     * @param indices    índice de cada columna reconocida dentro de la tabla
     * @param problemas  conflictos que impiden continuar, ya redactados
     * @param ignoradas  encabezados sin destino, que se avisan y se descartan
     */
    public record Emparejado(List<Columna> columnas,
                             Map<ColumnaMuestra, Integer> indices,
                             List<String> problemas,
                             List<String> ignoradas) {

        /** El índice de esa columna, o -1 si el archivo no la trae. */
        public int indiceDe(ColumnaMuestra campo) {
            return indices.getOrDefault(campo, -1);
        }

        public boolean trae(ColumnaMuestra campo) {
            return indiceDe(campo) >= 0;
        }

        public boolean sinProblemas() {
            return problemas.isEmpty();
        }
    }

    public static Emparejado emparejar(List<String> encabezados) {
        List<Columna> columnas = new ArrayList<>();
        List<String> problemas = new ArrayList<>();
        List<String> ignoradas = new ArrayList<>();
        Map<ColumnaMuestra, Integer> indices = new EnumMap<>(ColumnaMuestra.class);
        // Para poder decir cuál era la otra columna cuando dos apuntan a lo mismo.
        Map<ColumnaMuestra, String> primerEncabezado = new LinkedHashMap<>();

        for (int i = 0; i < encabezados.size(); i++) {
            String encabezado = encabezados.get(i);
            String clave = sinPuntuacion(NormalizadorAlias.normalizar(encabezado));

            if (clave == null || clave.isBlank()) {
                // Excel deja columnas sin título cuando alguien borra una celda del
                // encabezado. No es un error: simplemente no apunta a nada.
                columnas.add(new Columna(i, encabezado, null, false));
                continue;
            }
            if (INFORMATIVAS.contains(clave)) {
                columnas.add(new Columna(i, encabezado, null, true));
                continue;
            }

            ColumnaMuestra campo = porTitulo(clave);
            if (campo == null) {
                ignoradas.add(encabezado);
                columnas.add(new Columna(i, encabezado, null, false));
                continue;
            }
            if (indices.containsKey(campo)) {
                problemas.add("Las columnas \"" + primerEncabezado.get(campo) + "\" y \"" + encabezado
                        + "\" dicen lo mismo (" + campo.tituloPreferido() + "). Deje solo una.");
                columnas.add(new Columna(i, encabezado, null, false));
                continue;
            }

            indices.put(campo, i);
            primerEncabezado.put(campo, encabezado);
            columnas.add(new Columna(i, encabezado, campo, false));
        }

        for (ColumnaMuestra campo : values()) {
            if (campo.esObligatoria() && !indices.containsKey(campo)) {
                problemas.add("Falta la columna \"" + campo.tituloPreferido() + "\", que es obligatoria.");
            }
        }

        return new Emparejado(columnas, indices, problemas, ignoradas);
    }

    private static ColumnaMuestra porTitulo(String claveNormalizada) {
        return Arrays.stream(values())
                .filter(c -> c.titulos.contains(claveNormalizada))
                .findFirst()
                .orElse(null);
    }

    /**
     * Quita puntos, comas y símbolos del título ya normalizado.
     *
     * <p>Así «No. alícuota», «Nº ALICUOTA» y «no alicuota» caen todos en la misma
     * clave sin tener que enumerar cada variante. Se puede hacer sin riesgo
     * porque el conjunto de títulos es cerrado y está aquí: no hay alias escritos
     * por el usuario que puedan colisionar al limpiarlos, que es justamente por
     * lo que el emparejador de estudios no lo aplica a los suyos.</p>
     */
    private static String sinPuntuacion(String normalizado) {
        if (normalizado == null) return null;
        return normalizado.replaceAll("[^\\p{L}\\p{N} ]", "").replaceAll("\\s+", " ").trim();
    }
}
