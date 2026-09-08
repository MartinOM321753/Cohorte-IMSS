package imss.gob.mx.cohorte.services.formulas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Revisa una fórmula sin calcularla.
 *
 * <p>Es la misma pieza que usan el editor mientras se escribe y el servidor al
 * guardar. Que sea una sola es lo que evita que la pantalla acepte algo que el
 * servidor después rechaza, o al revés.</p>
 *
 * <h2>Dos niveles, y solo uno detiene</h2>
 * <p><b>Impide guardar</b> lo que no tiene arreglo posible: que la fórmula no se pueda
 * leer, que mencione una variable que nadie declaró, o que prometa una unidad de salida
 * que la operación demostradamente no produce.</p>
 *
 * <p><b>Solo advierte</b> todo lo demás. Si alguien suma centímetros con kilos, sale el
 * aviso y se guarda igual: la herramienta ofrece las operaciones y quien arma el
 * reporte decide qué tiene sentido para lo suyo. Fue una decisión explícita, y la
 * alternativa —que el sistema imponga su criterio— deja fuera cálculos legítimos que
 * nadie previó.</p>
 */
public final class ValidadorFormula {

    public enum Nivel {
        /** No se puede guardar así. */
        IMPIDE,
        /** Se puede guardar; conviene mirarlo. */
        ADVIERTE
    }

    /**
     * @param posicion dónde señalar en el texto, o −1 si el aviso no es de un punto
     */
    public record Aviso(Nivel nivel, String mensaje, int posicion) {

        public static Aviso impide(String mensaje, int posicion) {
            return new Aviso(Nivel.IMPIDE, mensaje, posicion);
        }

        public static Aviso advierte(String mensaje) {
            return new Aviso(Nivel.ADVIERTE, mensaje, -1);
        }
    }

    public record Revision(List<Aviso> avisos) {

        public boolean sePuedeGuardar() {
            return avisos.stream().noneMatch(a -> a.nivel() == Nivel.IMPIDE);
        }

        public List<Aviso> deNivel(Nivel nivel) {
            return avisos.stream().filter(a -> a.nivel() == nivel).toList();
        }
    }

    private ValidadorFormula() {}

    /**
     * @param texto             lo que escribió quien arma la fórmula
     * @param variables         las que declaró, con su clave y su unidad
     * @param unidadDeclarada   la que dijo que sale; {@code null} si no declaró ninguna
     */
    public static Revision revisar(String texto, List<VariableFormula> variables,
                                   Unidad unidadDeclarada) {
        List<Aviso> avisos = new ArrayList<>();

        Expresion arbol;
        try {
            arbol = AnalizadorFormula.analizar(texto);
        } catch (ErrorDeFormula e) {
            // Sin árbol no hay nada más que revisar: todo lo demás depende de poder
            // leer la fórmula.
            return new Revision(List.of(Aviso.impide(e.getMessage(), e.posicion())));
        }

        Map<String, VariableFormula> declaradas = new HashMap<>();
        if (variables != null) {
            for (VariableFormula v : variables) declaradas.put(v.nombre(), v);
        }

        revisarVariables(arbol, declaradas, avisos);
        revisarUnidades(arbol, declaradas, unidadDeclarada, avisos);
        return new Revision(List.copyOf(avisos));
    }

    // ── Variables ────────────────────────────────────────────────────────────

    private static void revisarVariables(Expresion arbol, Map<String, VariableFormula> declaradas,
                                         List<Aviso> avisos) {
        for (String usada : nombresUsados(arbol)) {
            if (!declaradas.containsKey(usada)) {
                avisos.add(Aviso.impide(
                        "La fórmula usa «" + usada + "», que no está en la lista de variables.", -1));
            }
        }

        for (VariableFormula v : declaradas.values()) {
            if (v.unidad() != null && v.unidad().dimension() == Dimension.DESCONOCIDA) {
                avisos.add(Aviso.advierte(
                        "La unidad de «" + v.nombre() + "» (" + v.unidad().nombre()
                                + ") no se reconoce, así que ese dato no se puede convertir a otra."));
            }
        }
    }

    /** Los nombres de variable que aparecen en la fórmula, sin repetir. */
    public static Set<String> nombresUsados(Expresion e) {
        Set<String> nombres = new LinkedHashSet<>();
        recolectar(e, nombres);
        return nombres;
    }

    private static void recolectar(Expresion e, Set<String> nombres) {
        switch (e) {
            case null -> { }
            case Expresion.Variable v -> nombres.add(v.nombre());
            case Expresion.Negacion n -> recolectar(n.sobre(), nombres);
            case Expresion.Operacion o -> {
                recolectar(o.izquierda(), nombres);
                recolectar(o.derecha(), nombres);
            }
            case Expresion.Comparacion c -> {
                recolectar(c.izquierda(), nombres);
                recolectar(c.derecha(), nombres);
            }
            case Expresion.Llamada l -> l.argumentos().forEach(a -> recolectar(a, nombres));
            case Expresion.Numero ignorado -> { }
            case Expresion.Texto ignorado -> { }
        }
    }

    // ── Unidades ─────────────────────────────────────────────────────────────

    private static void revisarUnidades(Expresion arbol, Map<String, VariableFormula> declaradas,
                                        Unidad unidadDeclarada, List<Aviso> avisos) {
        List<String> mezclas = new ArrayList<>();
        Optional<Unidad> resultante = deducir(arbol, declaradas, mezclas);

        for (String mezcla : mezclas) {
            avisos.add(Aviso.advierte(mezcla));
        }

        // Solo se impide cuando se sabe con certeza qué unidad produce la operación. En
        // cuanto hay una multiplicación o una división entre dos cantidades con unidad,
        // el resultado no tiene nombre —kilogramos entre metros al cuadrado no está en
        // ningún catálogo— y ahí no hay nada que contradecir.
        if (unidadDeclarada == null || Unidad.NINGUNA.equals(unidadDeclarada)) return;
        resultante.ifPresent(real -> {
            if (!real.equals(unidadDeclarada) && !Unidad.NINGUNA.equals(real)) {
                avisos.add(Aviso.impide(
                        "La fórmula declara que sale en " + unidadDeclarada.nombre()
                                + " y la operación da " + real.nombre() + ".", -1));
            }
        });
    }

    /**
     * Qué unidad produce la expresión, cuando se puede saber.
     *
     * <p>Vacío significa «no se puede afirmar», que es distinto de «no tiene unidad».
     * Se devuelve vacío en cuanto aparece una unidad derivada, porque nombrarla exigiría
     * un álgebra de dimensiones completa que aquí no aporta nada: quien escribe la
     * fórmula ya declaró en qué sale.</p>
     */
    private static Optional<Unidad> deducir(Expresion e, Map<String, VariableFormula> declaradas,
                                            List<String> mezclas) {
        return switch (e) {
            case null -> Optional.empty();

            case Expresion.Numero ignorado -> Optional.of(Unidad.NINGUNA);

            // Un texto no lleva unidad, y solo aparece dentro de una comparación, que
            // ya se resuelve como adimensional.
            case Expresion.Texto ignorado -> Optional.of(Unidad.NINGUNA);

            case Expresion.Variable v -> {
                VariableFormula d = declaradas.get(v.nombre());
                if (d == null) yield Optional.empty();
                yield Optional.of(d.unidad() != null ? d.unidad() : Unidad.NINGUNA);
            }

            case Expresion.Negacion n -> deducir(n.sobre(), declaradas, mezclas);

            case Expresion.Comparacion ignorado -> Optional.of(Unidad.NINGUNA);

            case Expresion.Operacion o -> {
                Optional<Unidad> a = deducir(o.izquierda(), declaradas, mezclas);
                Optional<Unidad> b = deducir(o.derecha(), declaradas, mezclas);
                if (a.isEmpty() || b.isEmpty()) yield Optional.empty();

                yield switch (o.operador()) {
                    case SUMA, RESTA -> {
                        if (!a.get().equals(b.get())) {
                            mezclas.add(avisoDeMezcla(o.operador(), a.get(), b.get()));
                            yield Optional.empty();
                        }
                        yield a;
                    }
                    case MULTIPLICACION, DIVISION -> {
                        if (Unidad.NINGUNA.equals(b.get())) yield a;
                        if (Unidad.NINGUNA.equals(a.get())) yield b;
                        yield Optional.empty();   // derivada, sin nombre
                    }
                    case POTENCIA -> Unidad.NINGUNA.equals(a.get()) ? a : Optional.empty();
                };
            }

            case Expresion.Llamada l -> {
                // «si» devuelve una de sus dos ramas; el resto se deja sin afirmar.
                if ("si".equals(l.funcion()) && l.argumentos().size() == 3) {
                    Optional<Unidad> siSe = deducir(l.argumentos().get(1), declaradas, mezclas);
                    Optional<Unidad> siNo = deducir(l.argumentos().get(2), declaradas, mezclas);
                    yield siSe.isPresent() && siSe.equals(siNo) ? siSe : Optional.empty();
                }
                yield Optional.empty();
            }
        };
    }

    private static String avisoDeMezcla(Expresion.Operador op, Unidad a, Unidad b) {
        String verbo = op == Expresion.Operador.SUMA ? "suma" : "resta";
        return "Se " + verbo + " " + nombreLegible(a) + " con " + nombreLegible(b)
                + ". Se puede hacer, pero conviene revisar que sea lo que se busca.";
    }

    private static String nombreLegible(Unidad u) {
        return u.nombre().isBlank() ? "un valor sin unidad" : u.nombre();
    }
}
