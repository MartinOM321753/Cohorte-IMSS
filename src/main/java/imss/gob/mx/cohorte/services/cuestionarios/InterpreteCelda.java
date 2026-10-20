package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.services.importacion.NormalizadorFecha;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Convierte el texto de una celda del volcado en una respuesta, o en el motivo por
 * el que no hay respuesta.
 *
 * <p>Es la pieza donde se concentra lo que costó entender del instrumento, y por eso
 * está sola y sin dependencias: recibe una variable y un texto, devuelve una
 * decisión. Así se puede razonar sobre ella —y corregirla— sin tocar el importador.</p>
 *
 * <h3>Las marcas de ausencia van antes que el tipo</h3>
 *
 * <p>Un {@code [UNAUTHORIZED]} en una celda de número no es un número ilegible: es
 * una celda que nunca se leyó. Si el tipo se comprobara primero, se reportaría como
 * «valor no numérico», que manda a buscar el error al lugar equivocado —al papel, en
 * vez de a la licencia de Azure—. Las marcas se reconocen primero y la celda sale de
 * aquí sin pasar por la conversión.</p>
 *
 * <h3>El cero es ambiguo y se resuelve por tipo</h3>
 *
 * <p>Un {@code 0} significa «Remark no contó marcas» en una casilla, pero en un campo
 * numérico puede ser la cantidad cero, que es una respuesta legítima: cero
 * cigarrillos, cero embarazos. Tratarlo igual en los dos casos borraría respuestas
 * reales o inventaría otras. Se interpreta según el tipo de la variable.</p>
 */
public final class InterpreteCelda {

    private InterpreteCelda() {}

    /** Lo que Remark escribe cuando declara la región en blanco. */
    private static final Set<String> EN_BLANCO = Set.of("BLANK", "EN BLANCO");

    /** El reconocimiento de escritura de Azure sin licencia. */
    private static final String NO_AUTORIZADO = "[UNAUTHORIZED]";

    /** Fallo de lectura de la región. El 3100 es el que trae el volcado recibido. */
    private static final Set<String> ERRORES_LECTURA = Set.of("ERROR3100", "ERROR");

    /**
     * El resultado de interpretar una celda.
     *
     * @param marca    por qué no hay valor, o {@link MarcaAusencia#NINGUNA}
     * @param codigo   código de la opción, cuando la variable es de opciones
     * @param numero   cantidad, cuando la variable es numérica
     * @param texto    texto reconocido
     * @param fecha    fecha
     * @param crudo    lo que decía la celda, literal
     * @param problema descripción del problema cuando el valor no encaja con el tipo
     *                 o con el catálogo; nulo si todo cuadra. No impide guardar:
     *                 perder el dato por no saber interpretarlo sería peor que
     *                 guardarlo señalado.
     */
    public record Interpretacion(
            MarcaAusencia marca,
            String codigo,
            Double numero,
            String texto,
            LocalDate fecha,
            String crudo,
            String problema
    ) {
        public boolean tieneProblema() {
            return problema != null;
        }
    }

    public static Interpretacion interpretar(VariableCuestionario variable, String celda) {
        String crudo = celda == null ? "" : celda.trim();
        TipoVariable tipo = variable.getTipo() == null
                ? TipoVariable.OPCION_UNICA : variable.getTipo();

        // ── Marcas de ausencia, antes que cualquier conversion ──────────────────
        if (crudo.isEmpty()) {
            // Una casilla vacia no es un dato que falte: es la respuesta. Quien no nada
            // deja vacia la casilla «Nadar Lunes», y contarla como «sin responder»
            // infla los faltantes de forma absurda —un participante que hace tres
            // actividades deja casi cien casillas vacias solo en la 112—.
            //
            // Se guarda como CERO_MARCAS y no como un «no» literal porque las dos cosas
            // no son iguales: si NINGUNA casilla del grupo trae marca, puede ser que no
            // realice la actividad o que el escaner no leyera esa franja, y esa
            // diferencia no se debe borrar aqui.
            return ausencia(esCasilla(tipo) ? MarcaAusencia.CERO_MARCAS
                    : MarcaAusencia.SIN_RESPUESTA, crudo);
        }
        String mayus = crudo.toUpperCase();
        if (EN_BLANCO.contains(mayus)) {
            return ausencia(MarcaAusencia.EN_BLANCO, crudo);
        }
        if (NO_AUTORIZADO.equals(mayus)) {
            return ausencia(MarcaAusencia.NO_AUTORIZADO, crudo);
        }
        if (ERRORES_LECTURA.contains(mayus) || mayus.startsWith("ERROR")) {
            return ausencia(MarcaAusencia.ERROR_LECTURA, crudo);
        }

        return switch (tipo) {
            case CASILLA, OPCION_MULTIPLE -> casilla(variable, crudo);
            case OPCION_UNICA -> opcionUnica(variable, crudo);
            case NUMERO -> numero(variable, crudo, null);
            case NUMERO_CON_ESCAPE -> numeroConEscape(variable, crudo);
            case FECHA -> fecha(crudo);
            case TEXTO -> texto(variable, crudo);
            // Un encabezado de grupo no se contesta. Si el volcado trae algo en su
            // columna, el que esta mal catalogado es el encabezado, no el dato: se
            // guarda el texto y se senala, que es lo que permite corregir el catalogo.
            case GRUPO -> new Interpretacion(MarcaAusencia.NINGUNA, null, null, crudo, null, crudo,
                    "La variable esta catalogada como encabezado de grupo pero el volcado trae valor.");
        };
    }

    /** Si el tipo se contesta marcando o no marcando, sin codigo que elegir. */
    private static boolean esCasilla(TipoVariable tipo) {
        return tipo == TipoVariable.CASILLA || tipo == TipoVariable.OPCION_MULTIPLE;
    }

    // ── Por tipo ─────────────────────────────────────────────────────────────

    /**
     * Casilla suelta o casilla de un grupo múltiple.
     *
     * <p>El cero es ausencia de marca, no un código: aquí sí, porque una casilla no
     * tiene una opción «0» en ningún catálogo del instrumento. Cualquier otro valor
     * se acepta como marca —el volcado escribe {@code 1}, y en algunas variables de
     * dos burbujas escribe {@code 2}— y se comprueba contra el catálogo si lo hay.</p>
     */
    private static Interpretacion casilla(VariableCuestionario variable, String crudo) {
        if ("0".equals(crudo)) {
            return ausencia(MarcaAusencia.CERO_MARCAS, crudo);
        }
        String codigo = normalizarCodigo(crudo);
        List<OpcionVariable> opciones = variable.getOpciones();
        if (opciones != null && !opciones.isEmpty() && !contieneCodigo(opciones, codigo)) {
            return new Interpretacion(MarcaAusencia.NINGUNA, codigo, null, null, null, crudo,
                    "El codigo " + codigo + " no esta entre las opciones de " + variable.getCodigo() + ".");
        }
        return new Interpretacion(MarcaAusencia.NINGUNA, codigo, null, null, null, crudo, null);
    }

    /**
     * Una opción de un catálogo cerrado.
     *
     * <p>Aquí el cero <em>no</em> se descarta de entrada: hay variables cuyo catálogo
     * incluye un código 0. Se comprueba contra las opciones, y solo si no está se
     * trata como «sin marcas».</p>
     */
    private static Interpretacion opcionUnica(VariableCuestionario variable, String crudo) {
        String codigo = normalizarCodigo(crudo);
        List<OpcionVariable> opciones = variable.getOpciones();

        if (opciones == null || opciones.isEmpty()) {
            // Sin catalogo no hay nada contra lo que comprobar. Se guarda el codigo
            // tal cual: es el caso de las variables que el volcado produce y ningun
            // catalogo documenta, y perderlas seria peor que guardarlas sin validar.
            return new Interpretacion(MarcaAusencia.NINGUNA, codigo, null, null, null, crudo, null);
        }
        if (contieneCodigo(opciones, codigo)) {
            return new Interpretacion(MarcaAusencia.NINGUNA, codigo, null, null, null, crudo, null);
        }
        if ("0".equals(codigo)) {
            return ausencia(MarcaAusencia.CERO_MARCAS, crudo);
        }
        return new Interpretacion(MarcaAusencia.NINGUNA, codigo, null, null, null, crudo,
                "El codigo " + codigo + " no esta entre las " + opciones.size()
                        + " opciones de " + variable.getCodigo() + ".");
    }

    private static Interpretacion numero(VariableCuestionario variable, String crudo, String codigoEscape) {
        Double valor = aDouble(crudo);
        if (valor == null) {
            return new Interpretacion(MarcaAusencia.NINGUNA, null, null, crudo, null, crudo,
                    "Se esperaba un numero y la celda dice \"" + crudo + "\".");
        }
        String problema = fueraDeRango(variable, valor);
        return new Interpretacion(MarcaAusencia.NINGUNA, codigoEscape, valor, null, null, crudo, problema);
    }

    /**
     * Número que admite un código de escape por fuera del rango.
     *
     * <p>El escape se reconoce por el catálogo, no por ser un número alto: dar por
     * escape todo lo que pase del máximo convertiría un peso mal burbujeado en «no
     * sabe», que es una respuesta que el participante no dio.</p>
     */
    private static Interpretacion numeroConEscape(VariableCuestionario variable, String crudo) {
        String codigo = normalizarCodigo(crudo);
        List<OpcionVariable> opciones = variable.getOpciones();
        if (opciones != null && contieneCodigo(opciones, codigo)) {
            return new Interpretacion(MarcaAusencia.NINGUNA, codigo, null, null, null, crudo, null);
        }
        return numero(variable, crudo, null);
    }

    private static Interpretacion fecha(String crudo) {
        LocalDate valor = aFecha(crudo);
        if (valor == null) {
            return new Interpretacion(MarcaAusencia.NINGUNA, null, null, crudo, null, crudo,
                    "Se esperaba una fecha y la celda dice \"" + crudo + "\".");
        }
        return new Interpretacion(MarcaAusencia.NINGUNA, null, null, null, valor, crudo, null);
    }

    private static Interpretacion texto(VariableCuestionario variable, String crudo) {
        Integer tope = variable.getLongitudMax();
        String problema = (tope != null && tope > 0 && crudo.length() > tope)
                ? "El texto tiene " + crudo.length() + " caracteres y en el papel caben " + tope + "."
                : null;
        return new Interpretacion(MarcaAusencia.NINGUNA, null, null, crudo, null, crudo, problema);
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private static Interpretacion ausencia(MarcaAusencia marca, String crudo) {
        return new Interpretacion(marca, null, null, null, null, crudo, null);
    }

    /**
     * Quita el {@code .0} que arrastra un código leído como número.
     *
     * <p>Un {@code 7} que pasó por una celda numérica de Excel puede llegar como
     * {@code 7.0}, y entonces no coincide con la opción {@code 7} del catálogo. El
     * lector ya cubre el caso habitual, pero el código puede venir de un CSV o de
     * una captura, y sin esto el fallo sería «el codigo 7.0 no esta entre las
     * opciones», que no dice nada de lo que pasa.</p>
     */
    private static String normalizarCodigo(String crudo) {
        if (crudo.endsWith(".0")) {
            String sinDecimal = crudo.substring(0, crudo.length() - 2);
            if (sinDecimal.matches("-?\\d+")) return sinDecimal;
        }
        return crudo;
    }

    private static boolean contieneCodigo(List<OpcionVariable> opciones, String codigo) {
        for (OpcionVariable o : opciones) {
            if (o.getCodigo() != null && o.getCodigo().equals(codigo)) return true;
        }
        return false;
    }

    private static String fueraDeRango(VariableCuestionario variable, Double valor) {
        Double min = variable.getValorMin();
        Double max = variable.getValorMax();
        if (min != null && valor < min) {
            return "El valor " + valor + " esta por debajo del minimo " + min + ".";
        }
        if (max != null && valor > max) {
            return "El valor " + valor + " supera el maximo " + max + ".";
        }
        return null;
    }

    private static Double aDouble(String crudo) {
        try {
            return Double.valueOf(crudo.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * La fecha de la celda.
     *
     * <h3>Esto estaba mal y así se rompió</h3>
     *
     * <p>Aquí vivía un conversor propio que admitía dos formatos: ISO —lo que el lector
     * entrega para las celdas con formato de fecha de Excel— y {@code dd/mm/aaaa} con
     * barras. El volcado real de Remark manda las fechas como texto con guiones,
     * {@code 13-06-1968}, y ninguno de los dos las aceptaba: fechas perfectamente
     * legibles y sin ambigüedad quedaban marcadas como «se esperaba una fecha». El
     * dato no se perdía —el valor literal se conserva— pero obligaba a recapturar a
     * mano algo que el archivo traía bien.</p>
     *
     * <p>Lo peor del caso es que el conversor no hacía falta: el sistema ya tenía
     * {@link NormalizadorFecha}, que es el que usan las demás cargas masivas y que
     * cubre ISO, barras, guiones, el mes en letra y los dos avisos que importan —año de
     * dos cifras y fecha inexistente—. Tener dos conversores de fecha garantizaba que
     * uno se quedara atrás, y el que se quedó atrás era este.</p>
     *
     * <h3>Por qué aquí el orden es día/mes y no se deduce</h3>
     *
     * <p>{@link NormalizadorFecha} permite deducir del archivo si las fechas vienen
     * día/mes o mes/día. Para un volcado de papel eso no procede: las casillas de la
     * hoja están rotuladas día, mes y año en ese orden, así que una celda que solo
     * tenga sentido al revés —{@code 06-13-1968}— no es otra convención, es un error de
     * captura. Deducir el orden la daría por buena cambiada de mes; fijarlo la deja sin
     * convertir y señalada, que es lo que permite ir a la hoja y comprobarla.</p>
     */
    private static LocalDate aFecha(String crudo) {
        try {
            return NormalizadorFecha.parsear(crudo, NormalizadorFecha.Orden.DIA_MES)
                    .toLocalDate();
        } catch (RuntimeException noEsFecha) {
            return null;
        }
    }
}
