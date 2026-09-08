package imss.gob.mx.cohorte.services.formulas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Calcula una fórmula ya analizada.
 *
 * <p>No conoce participantes ni base de datos: recibe de fuera con qué contestar cada
 * variable. Así el mismo evaluador sirve para el reporte de alguien real, para la
 * vista previa del editor y para las pruebas, sin montar nada alrededor.</p>
 *
 * <h2>La ausencia manda</h2>
 * <p>Si falta cualquier dato que la operación necesita, el resultado es ausente y se
 * contagia hacia arriba hasta la celda, que sale en blanco. Nunca cero: un
 * participante sin peso registrado no tiene un índice de masa corporal de cero, tiene
 * un índice que no se puede calcular, y la diferencia se lee en un documento clínico.
 * Por la misma razón dividir entre cero da ausente en vez de reventar.</p>
 *
 * <h2>Las unidades no se inventan</h2>
 * <p>Al sumar dos cantidades de la misma unidad, el resultado la conserva. En cuanto
 * se multiplica o se divide, la unidad del resultado deja de tener nombre —kilogramos
 * entre metros al cuadrado no está en ningún catálogo— y se devuelve sin unidad. La
 * que se imprime es la que declaró quien escribió la fórmula, y esa decisión es suya:
 * aquí no se adivina ni se corrige.</p>
 */
public final class Evaluador {

    /**
     * Tope del exponente.
     *
     * <p>Elevar a un número grande genera cifras enormes y come memoria y tiempo. Las
     * fórmulas clínicas elevan al cuadrado o al cubo; treinta y dos deja margen de
     * sobra y cierra la puerta a que una fórmula guardada tumbe la emisión.</p>
     */
    static final int EXPONENTE_MAXIMO = 32;

    private Evaluador() {}

    /**
     * @param variables con qué contestar cada nombre; devolver ausente o {@code null}
     *                  para las que no se sepan resolver
     */
    public static Magnitud evaluar(Expresion e, Function<String, Magnitud> variables) {
        if (e == null) return Magnitud.sinDato();

        return switch (e) {
            case Expresion.Numero n -> Magnitud.de(n.valor(), Unidad.NINGUNA);

            case Expresion.Texto t -> Magnitud.deTexto(t.valor());

            case Expresion.Variable v -> {
                Magnitud m = variables == null ? null : variables.apply(v.nombre());
                yield m != null ? m : Magnitud.sinDato();
            }

            case Expresion.Negacion neg -> {
                Magnitud m = evaluar(neg.sobre(), variables);
                yield m.ausente() ? m : Magnitud.de(m.valor().negate(), m.unidad());
            }

            case Expresion.Operacion o -> operar(o, variables);

            case Expresion.Comparacion c -> comparar(c, variables);

            case Expresion.Llamada l -> llamar(l, variables);
        };
    }

    // ── Operaciones ──────────────────────────────────────────────────────────

    private static Magnitud operar(Expresion.Operacion o, Function<String, Magnitud> variables) {
        Magnitud a = evaluar(o.izquierda(), variables);
        if (a.ausente()) return Magnitud.sinDato();

        Magnitud b = evaluar(o.derecha(), variables);
        if (b.ausente()) return Magnitud.sinDato();

        // Un texto no se suma ni se divide. Se deja en blanco en vez de intentar
        // convertirlo a número: «Derecha» no vale cero, no vale nada.
        if (a.esTexto() || b.esTexto()) return Magnitud.sinDato();

        return switch (o.operador()) {
            case SUMA -> Magnitud.de(a.valor().add(b.valor()), unidadAlSumar(a, b));
            case RESTA -> Magnitud.de(a.valor().subtract(b.valor()), unidadAlSumar(a, b));
            case MULTIPLICACION -> Magnitud.de(a.valor().multiply(b.valor()), unidadAlEscalar(a, b));
            case DIVISION -> dividir(a, b);
            case POTENCIA -> elevar(a, b);
        };
    }

    private static Magnitud dividir(Magnitud a, Magnitud b) {
        // Dividir entre cero no es un error del que haya que avisar: es que con estos
        // datos el valor no existe. Sale en blanco, como cualquier otro hueco.
        if (b.valor().signum() == 0) return Magnitud.sinDato();
        return Magnitud.de(a.valor().divide(b.valor(), Magnitud.PRECISION), unidadAlEscalar(a, b));
    }

    private static Magnitud elevar(Magnitud base, Magnitud exponente) {
        BigDecimal e = exponente.valor().stripTrailingZeros();

        // Solo exponentes enteros. Una raíz se pide con «raiz(...)», que es más claro
        // de leer que elevar a 0.5 y no arrastra el error de una potencia fraccionaria.
        if (e.scale() > 0) return Magnitud.sinDato();

        int n;
        try {
            n = e.intValueExact();
        } catch (ArithmeticException noCabe) {
            return Magnitud.sinDato();
        }
        if (Math.abs(n) > EXPONENTE_MAXIMO) return Magnitud.sinDato();

        // Elevar metros da metros al cuadrado: sigue habiendo unidad, aunque no tenga
        // nombre. Marcarla como «sin unidad» haría que la división posterior heredara
        // los kilos y «peso ÷ estatura²» acabara etiquetado en kg.
        Unidad resultante = sinUnidad(base) ? Unidad.NINGUNA : Unidad.DERIVADA;

        if (n >= 0) {
            return Magnitud.de(base.valor().pow(n), resultante);
        }
        if (base.valor().signum() == 0) return Magnitud.sinDato();
        BigDecimal positiva = base.valor().pow(-n);
        return Magnitud.de(BigDecimal.ONE.divide(positiva, Magnitud.PRECISION), resultante);
    }

    /**
     * Sumar conserva la unidad solo si las dos son la misma.
     *
     * <p>Sumar centímetros con kilos se permite —esa fue una decisión explícita— pero
     * el resultado no es ni centímetros ni kilos, así que se queda sin unidad en lugar
     * de heredar la de la izquierda, que sería mentir sobre lo que hay.</p>
     */
    private static Unidad unidadAlSumar(Magnitud a, Magnitud b) {
        return a.unidad().equals(b.unidad()) ? a.unidad() : Unidad.NINGUNA;
    }

    /**
     * Multiplicar o dividir por un número sin unidad no cambia la unidad.
     *
     * <p>«peso × 2» sigue siendo kilos. «peso ÷ estatura²» ya no es nada que el
     * catálogo sepa nombrar, y ahí se devuelve sin unidad.</p>
     */
    private static Unidad unidadAlEscalar(Magnitud a, Magnitud b) {
        if (sinUnidad(b)) return a.unidad();
        if (sinUnidad(a)) return b.unidad();
        return Unidad.DERIVADA;
    }

    /**
     * Un número pelado, sin unidad de ninguna clase.
     *
     * <p>Es distinto de tener una unidad sin nombre: el 2 de «peso × 2» no aporta
     * unidad y deja pasar los kilos, mientras que los metros al cuadrado de
     * «peso ÷ estatura²» sí son una unidad y por eso el resultado deja de ser kilos.</p>
     */
    private static boolean sinUnidad(Magnitud m) {
        return m.unidad() == null || Unidad.NINGUNA.equals(m.unidad());
    }

    // ── Comparaciones ────────────────────────────────────────────────────────

    /**
     * Una comparación vale 1 cuando se cumple y 0 cuando no.
     *
     * <p>Se representa como número para que todo el motor trabaje con un solo tipo. Lo
     * usa «si», y quien quiera sumarlas para contar cuántas condiciones se cumplen
     * puede hacerlo.</p>
     */
    private static Magnitud comparar(Expresion.Comparacion c, Function<String, Magnitud> variables) {
        Magnitud a = evaluar(c.izquierda(), variables);
        if (a.ausente()) return Magnitud.sinDato();
        Magnitud b = evaluar(c.derecha(), variables);
        if (b.ausente()) return Magnitud.sinDato();

        if (a.esTexto() || b.esTexto()) return compararTextos(c.comparador(), a, b);

        int signo = a.valor().compareTo(b.valor());
        boolean cumple = switch (c.comparador()) {
            case IGUAL       -> signo == 0;
            case DISTINTO    -> signo != 0;
            case MENOR       -> signo < 0;
            case MENOR_IGUAL -> signo <= 0;
            case MAYOR       -> signo > 0;
            case MAYOR_IGUAL -> signo >= 0;
        };
        return Magnitud.de(cumple ? BigDecimal.ONE : BigDecimal.ZERO, Unidad.NINGUNA);
    }

    /**
     * Comparar una opción con un texto.
     *
     * <p>Solo «igual» y «distinto»: preguntar si «Derecha» es menor que «Izquierda» no
     * significa nada, y contestar que sí por orden alfabético sería inventar un
     * criterio. Esos casos quedan sin dato, que es lo mismo que hace el motor siempre
     * que no puede responder.</p>
     *
     * <p>Se compara sin distinguir mayúsculas ni espacios sobrantes. Las opciones se
     * capturan a mano en el catálogo y «Derecha» y «derecha» son la misma; que una
     * regla clínica dependiera de eso sería una trampa.</p>
     */
    private static Magnitud compararTextos(Expresion.Comparador comparador,
                                           Magnitud a, Magnitud b) {
        String uno = a.texto(null).trim();
        String otro = b.texto(null).trim();

        boolean iguales = uno.equalsIgnoreCase(otro);
        return switch (comparador) {
            case IGUAL    -> Magnitud.de(iguales ? BigDecimal.ONE : BigDecimal.ZERO, Unidad.NINGUNA);
            case DISTINTO -> Magnitud.de(iguales ? BigDecimal.ZERO : BigDecimal.ONE, Unidad.NINGUNA);
            default -> Magnitud.sinDato();
        };
    }

    // ── Funciones ────────────────────────────────────────────────────────────

    private static Magnitud llamar(Expresion.Llamada l, Function<String, Magnitud> variables) {
        List<Expresion> args = l.argumentos();

        // «si» evalúa solo la rama que corresponde: la otra puede depender de un dato
        // que este participante no tiene, y calcularla la volvería ausente sin motivo.
        if ("si".equals(l.funcion())) {
            Magnitud condicion = evaluar(args.get(0), variables);
            if (condicion.ausente()) return Magnitud.sinDato();
            boolean cumple = condicion.valor().signum() != 0;
            return evaluar(cumple ? args.get(1) : args.get(2), variables);
        }

        List<Magnitud> valores = new ArrayList<>(args.size());
        for (Expresion arg : args) {
            Magnitud m = evaluar(arg, variables);
            // Si falta uno, el mínimo del conjunto no se sabe. Ignorarlo daría el
            // mínimo de los que sí están, que es otra cosa y no se distinguiría.
            if (m.ausente()) return Magnitud.sinDato();
            valores.add(m);
        }

        return switch (l.funcion()) {
            case "abs" -> Magnitud.de(valores.get(0).valor().abs(), valores.get(0).unidad());
            case "raiz" -> raiz(valores.get(0));
            case "redondear" -> redondear(valores.get(0), valores.get(1));
            case "min" -> extremo(valores, true);
            case "max" -> extremo(valores, false);
            case "promedio" -> promedio(valores);
            default -> Magnitud.sinDato();
        };
    }

    private static Magnitud raiz(Magnitud m) {
        if (m.valor().signum() < 0) return Magnitud.sinDato();
        return Magnitud.de(m.valor().sqrt(Magnitud.PRECISION), Unidad.NINGUNA);
    }

    private static Magnitud redondear(Magnitud valor, Magnitud decimales) {
        int n;
        try {
            n = decimales.valor().intValueExact();
        } catch (ArithmeticException noEsEntero) {
            return Magnitud.sinDato();
        }
        if (n < 0 || n > 10) return Magnitud.sinDato();
        return Magnitud.de(valor.valor().setScale(n, RoundingMode.HALF_UP), valor.unidad());
    }

    private static Magnitud extremo(List<Magnitud> valores, boolean menor) {
        Magnitud elegido = valores.get(0);
        for (Magnitud m : valores) {
            int signo = m.valor().compareTo(elegido.valor());
            if (menor ? signo < 0 : signo > 0) elegido = m;
        }
        return elegido;
    }

    private static Magnitud promedio(List<Magnitud> valores) {
        BigDecimal suma = BigDecimal.ZERO;
        Unidad unidad = valores.get(0).unidad();
        for (Magnitud m : valores) {
            suma = suma.add(m.valor());
            if (!unidad.equals(m.unidad())) unidad = Unidad.NINGUNA;
        }
        return Magnitud.de(
                suma.divide(BigDecimal.valueOf(valores.size()), Magnitud.PRECISION), unidad);
    }
}
