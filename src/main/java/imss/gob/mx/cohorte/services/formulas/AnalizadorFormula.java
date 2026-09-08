package imss.gob.mx.cohorte.services.formulas;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Convierte el texto de una fórmula en el árbol que se puede calcular.
 *
 * <p>Lee de izquierda a derecha, un carácter a la vez, respetando la precedencia
 * habitual: primero la potencia, luego multiplicar y dividir, luego sumar y restar, y
 * al final la comparación. «18.5 × talla ^ 2» eleva antes de multiplicar, como en
 * cualquier calculadora.</p>
 *
 * <p>Se acepta escribir los operadores de las dos formas —{@code *} o {@code ×},
 * {@code /} o {@code ÷}, {@code <=} o {@code ≤}— porque el editor muestra los signos
 * bonitos y la gente escribe los del teclado.</p>
 *
 * <p><b>Los límites de tamaño no son decoración.</b> Estas fórmulas llegan de la base
 * de datos y se evalúan una vez por participante: sin un tope, una expresión con miles
 * de paréntesis anidados tumba el hilo que emite el reporte. Se cortan aquí, al leer,
 * que es antes de guardarlas.</p>
 */
public final class AnalizadorFormula {

    /** Largo máximo del texto de una fórmula. */
    static final int LARGO_MAXIMO = 2000;

    /** Cuántos paréntesis se pueden anidar. */
    static final int PROFUNDIDAD_MAXIMA = 32;

    /** Las funciones que se pueden usar. Fuera de esta lista no hay nada. */
    static final Set<String> FUNCIONES = Set.of(
            "si", "min", "max", "abs", "redondear", "raiz", "promedio");

    private final String texto;
    private int i;
    private int profundidad;

    private AnalizadorFormula(String texto) {
        this.texto = texto;
    }

    /**
     * Analiza el texto y devuelve el árbol.
     *
     * @throws ErrorDeFormula si el texto no se puede leer
     */
    public static Expresion analizar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ErrorDeFormula("La fórmula está vacía.");
        }
        if (texto.length() > LARGO_MAXIMO) {
            throw new ErrorDeFormula(
                    "La fórmula es demasiado larga: " + texto.length()
                            + " caracteres, y el máximo son " + LARGO_MAXIMO + ".");
        }

        AnalizadorFormula a = new AnalizadorFormula(texto);
        Expresion e = a.comparacion();
        a.saltarEspacios();
        if (a.i < texto.length()) {
            throw new ErrorDeFormula(
                    "Sobra «" + texto.substring(a.i).trim() + "» al final de la fórmula.", a.i);
        }
        return e;
    }

    // ── Niveles de precedencia, de menor a mayor ─────────────────────────────

    private Expresion comparacion() {
        Expresion izquierda = suma();
        saltarEspacios();

        Expresion.Comparador comp = leerComparador();
        if (comp == null) return izquierda;

        Expresion derecha = suma();
        return new Expresion.Comparacion(comp, izquierda, derecha);
    }

    private Expresion suma() {
        Expresion e = producto();
        while (true) {
            saltarEspacios();
            char c = actual();
            if (c == '+') { avanzar(); e = op(Expresion.Operador.SUMA, e, producto()); }
            else if (esSignoResta(c)) { avanzar(); e = op(Expresion.Operador.RESTA, e, producto()); }
            else return e;
        }
    }

    private Expresion producto() {
        Expresion e = potencia();
        while (true) {
            saltarEspacios();
            char c = actual();
            if (c == '*' || c == '×' || c == '·') {
                avanzar(); e = op(Expresion.Operador.MULTIPLICACION, e, potencia());
            } else if (c == '/' || c == '÷') {
                avanzar(); e = op(Expresion.Operador.DIVISION, e, potencia());
            } else {
                return e;
            }
        }
    }

    /** La potencia asocia a la derecha: «2^3^2» son dos elevado a nueve. */
    private Expresion potencia() {
        Expresion base = unario();
        saltarEspacios();
        if (actual() == '^') {
            avanzar();
            return op(Expresion.Operador.POTENCIA, base, potencia());
        }
        // «talla²» se escribe también con el superíndice, que es como lo muestra el
        // documento del que salen estas fórmulas.
        char c = actual();
        if (c == '²' || c == '³') {
            avanzar();
            BigDecimal exponente = BigDecimal.valueOf(c == '²' ? 2 : 3);
            return op(Expresion.Operador.POTENCIA, base, new Expresion.Numero(exponente));
        }
        return base;
    }

    private Expresion unario() {
        saltarEspacios();
        char c = actual();
        if (esSignoResta(c)) { avanzar(); return new Expresion.Negacion(unario()); }
        if (c == '+') { avanzar(); return unario(); }
        return primario();
    }

    private Expresion primario() {
        saltarEspacios();
        if (i >= texto.length()) {
            throw new ErrorDeFormula("La fórmula termina donde se esperaba un valor.", i);
        }

        char c = actual();

        if (c == '(') {
            entrar();
            avanzar();
            Expresion dentro = comparacion();
            saltarEspacios();
            if (actual() != ')') {
                throw new ErrorDeFormula("Falta cerrar un paréntesis.", i);
            }
            avanzar();
            salir();
            return dentro;
        }

        if (c == '\'' || c == '"') return textoEntreComillas(c);
        if (Character.isDigit(c) || c == '.') return numero();
        if (esInicioDeNombre(c)) return nombre();

        throw new ErrorDeFormula("No se entiende el símbolo «" + c + "».", i);
    }

    // ── Piezas sueltas ───────────────────────────────────────────────────────

    /**
     * Un texto entre comillas, simples o dobles.
     *
     * <p>Se admiten las dos porque la fórmula viaja dentro de un JSON hasta el
     * servidor: quien la escriba a mano con comillas dobles lo tendrá más incómodo, y
     * quien use el asistente ni se entera de cuáles se pusieron.</p>
     */
    private Expresion textoEntreComillas(char comilla) {
        avanzar();
        int desde = i;
        while (i < texto.length() && texto.charAt(i) != comilla) i++;
        if (i >= texto.length()) {
            throw new ErrorDeFormula("Falta cerrar unas comillas.", desde - 1);
        }
        String contenido = texto.substring(desde, i);
        avanzar();
        return new Expresion.Texto(contenido);
    }

    private Expresion numero() {
        int desde = i;
        boolean punto = false;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (Character.isDigit(c)) { i++; continue; }
            if (c == '.' && !punto) { punto = true; i++; continue; }
            break;
        }
        String crudo = texto.substring(desde, i);
        try {
            return new Expresion.Numero(new BigDecimal(crudo));
        } catch (NumberFormatException e) {
            throw new ErrorDeFormula("«" + crudo + "» no es un número.", desde);
        }
    }

    private Expresion nombre() {
        int desde = i;
        while (i < texto.length() && esParteDeNombre(texto.charAt(i))) i++;
        String nombre = texto.substring(desde, i);

        saltarEspacios();
        if (actual() != '(') return new Expresion.Variable(nombre);

        String funcion = nombre.toLowerCase(java.util.Locale.ROOT);
        if (!FUNCIONES.contains(funcion)) {
            throw new ErrorDeFormula(
                    "No existe la función «" + nombre + "». Las que se pueden usar son: "
                            + String.join(", ", new java.util.TreeSet<>(FUNCIONES)) + ".", desde);
        }

        entrar();
        avanzar(); // el paréntesis de apertura
        List<Expresion> argumentos = new ArrayList<>();
        saltarEspacios();
        if (actual() != ')') {
            argumentos.add(comparacion());
            saltarEspacios();
            while (actual() == ',' || actual() == ';') {
                avanzar();
                argumentos.add(comparacion());
                saltarEspacios();
            }
        }
        if (actual() != ')') {
            throw new ErrorDeFormula("Falta cerrar el paréntesis de «" + nombre + "».", i);
        }
        avanzar();
        salir();

        verificarArgumentos(funcion, argumentos.size(), desde);
        return new Expresion.Llamada(funcion, List.copyOf(argumentos));
    }

    /**
     * Que la función reciba los argumentos que necesita.
     *
     * <p>Se comprueba al leer y no al calcular porque es un error de escritura: quien
     * escribió {@code redondear(x)} sin decir cuántos decimales tiene que enterarse en
     * ese momento, no cuando ya se está emitiendo el reporte de alguien.</p>
     */
    private void verificarArgumentos(String funcion, int cuantos, int posicion) {
        String problema = switch (funcion) {
            case "si"        -> cuantos == 3 ? null : "«si» necesita tres: la condición, el valor si se cumple y el valor si no.";
            case "abs", "raiz" -> cuantos == 1 ? null : "«" + funcion + "» necesita un solo valor.";
            case "redondear" -> cuantos == 2 ? null : "«redondear» necesita dos: el valor y cuántos decimales.";
            case "min", "max", "promedio" -> cuantos >= 1 ? null : "«" + funcion + "» necesita al menos un valor.";
            default -> null;
        };
        if (problema != null) throw new ErrorDeFormula(problema, posicion);
    }

    private Expresion.Comparador leerComparador() {
        char c = actual();
        char siguiente = i + 1 < texto.length() ? texto.charAt(i + 1) : '\0';

        if (c == '<' && siguiente == '=') { i += 2; return Expresion.Comparador.MENOR_IGUAL; }
        if (c == '>' && siguiente == '=') { i += 2; return Expresion.Comparador.MAYOR_IGUAL; }
        if (c == '<' && siguiente == '>') { i += 2; return Expresion.Comparador.DISTINTO; }
        if (c == '!' && siguiente == '=') { i += 2; return Expresion.Comparador.DISTINTO; }
        if (c == '=' && siguiente == '=') { i += 2; return Expresion.Comparador.IGUAL; }
        if (c == '≤') { avanzar(); return Expresion.Comparador.MENOR_IGUAL; }
        if (c == '≥') { avanzar(); return Expresion.Comparador.MAYOR_IGUAL; }
        if (c == '≠') { avanzar(); return Expresion.Comparador.DISTINTO; }
        if (c == '<') { avanzar(); return Expresion.Comparador.MENOR; }
        if (c == '>') { avanzar(); return Expresion.Comparador.MAYOR; }
        if (c == '=') { avanzar(); return Expresion.Comparador.IGUAL; }
        return null;
    }

    // ── Utilidades del recorrido ─────────────────────────────────────────────

    private Expresion op(Expresion.Operador o, Expresion a, Expresion b) {
        return new Expresion.Operacion(o, a, b);
    }

    private char actual() {
        return i < texto.length() ? texto.charAt(i) : '\0';
    }

    private void avanzar() {
        i++;
    }

    private void saltarEspacios() {
        while (i < texto.length() && Character.isWhitespace(texto.charAt(i))) i++;
    }

    private void entrar() {
        if (++profundidad > PROFUNDIDAD_MAXIMA) {
            throw new ErrorDeFormula(
                    "La fórmula tiene demasiados paréntesis anidados; el máximo son "
                            + PROFUNDIDAD_MAXIMA + ".", i);
        }
    }

    private void salir() {
        profundidad--;
    }

    /** El menos del teclado y el menos tipográfico que pega un procesador de texto. */
    private static boolean esSignoResta(char c) {
        return c == '-' || c == '−' || c == '–';
    }

    private static boolean esInicioDeNombre(char c) {
        return Character.isLetter(c) || c == '_';
    }

    /**
     * Los nombres admiten puntos y dígitos porque una variable puede ser la clave de
     * un campo del catálogo, como {@code estudio.7.param.85}.
     */
    private static boolean esParteDeNombre(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }
}
