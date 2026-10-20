package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacion;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.Severidad;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.services.importacion.NormalizadorFecha;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Ejecuta las comprobaciones que la plantilla declara.
 *
 * <h3>Qué espera cada regla</h3>
 *
 * <ul>
 *   <li>{@code EXCLUYE} — {@code pregunta} es la opción de descarte y {@code valor}
 *       el código que la marca; {@code comparaCon} dice a quién excluye: el nombre de
 *       un bloque, o varias preguntas separadas por {@code |}.</li>
 *   <li>{@code REQUIERE_TEXTO} — {@code pregunta} es la casilla, {@code valor} el
 *       código que obliga y {@code comparaCon} la pregunta de texto que hay que
 *       llenar. Con {@code valor = *} basta cualquier respuesta.</li>
 *   <li>{@code SOLO_SEXO} — {@code pregunta} es una pregunta o un bloque entero,
 *       {@code valor} es {@code HOMBRES} o {@code MUJERES} y {@code comparaCon} la
 *       pregunta que lleva el sexo (en el general, {@code P3}).</li>
 *   <li>{@code FECHA_NO_FUTURA} — solo {@code pregunta}.</li>
 *   <li>{@code NO_MAYOR_QUE} — {@code pregunta} contra {@code comparaCon}, que es
 *       otra pregunta numérica o el literal {@code EDAD}.</li>
 * </ul>
 *
 * <h3>Lo que no hace, y es lo más importante</h3>
 *
 * <p><b>No corrige ni descarta nada.</b> Devuelve una lista de avisos y quien llama
 * decide: la pantalla de captura impide guardar si hay alguno de severidad
 * {@code ERROR}, y la importación los guarda todos como señal sin tocar el dato. Un
 * motor que «arreglara» respuestas destruiría justo la información que hace falta
 * para saber que la aplicación del cuestionario salió mal.</p>
 *
 * <h3>Y por qué calla cuando no sabe</h3>
 *
 * <p>Si falta el dato con el que comparar —no se contestó el sexo, no hay fecha de
 * nacimiento para calcular la edad— la regla no se pronuncia. Avisar ahí llenaría la
 * pantalla de advertencias que el participante no puede resolver, y enseñaría a no
 * leerlas.</p>
 */
public final class MotorValidaciones {

    private MotorValidaciones() {}

    /** Con este valor, {@code REQUIERE_TEXTO} se dispara con cualquier respuesta. */
    private static final String CUALQUIERA = "*";

    /**
     * Un incumplimiento.
     *
     * @param codigo    la pregunta a la que señalar en la pantalla
     * @param severidad si impide guardar o solo se anota
     */
    public record Aviso(String codigo, Severidad severidad, String mensaje) {}

    /**
     * @param respuestas lo contestado, por código de pregunta. Una pregunta sin
     *                   contestar no está en el mapa o está vacía
     * @param variables  el catálogo por código, para saber tipos y bloques
     * @param edad       años del participante, o null si no se puede saber
     */
    public static List<Aviso> revisar(List<ReglaValidacion> reglas,
                                      Map<String, String> respuestas,
                                      Map<String, VariableCuestionario> variables,
                                      Integer edad) {
        List<Aviso> avisos = new ArrayList<>();
        for (ReglaValidacion regla : reglas) {
            if (!Boolean.TRUE.equals(regla.getActiva())) continue;
            Aviso aviso = switch (regla.getRegla()) {
                case EXCLUYE -> excluye(regla, respuestas, variables);
                case REQUIERE_TEXTO -> requiereTexto(regla, respuestas);
                case SOLO_SEXO -> soloSexo(regla, respuestas, variables);
                case FECHA_NO_FUTURA -> fechaNoFutura(regla, respuestas);
                case NO_MAYOR_QUE -> noMayorQue(regla, respuestas, edad);
            };
            if (aviso != null) avisos.add(aviso);
        }
        return avisos;
    }

    // ── Las cinco ────────────────────────────────────────────────────────────

    private static Aviso excluye(ReglaValidacion regla, Map<String, String> respuestas,
                                 Map<String, VariableCuestionario> variables) {
        if (!coincide(respuestas.get(regla.getPregunta()), regla.getValor())) return null;

        // Marcó el descarte. Si además hay algo contestado de lo que excluye, hay
        // conflicto.
        for (String codigo : aQuienExcluye(regla, variables)) {
            if (codigo.equals(regla.getPregunta())) continue;
            if (tieneValor(respuestas.get(codigo))) return aviso(regla);
        }
        return null;
    }

    /**
     * A quién excluye la opción de descarte: un bloque entero o unas preguntas
     * concretas.
     *
     * <h3>Por qué no basta con el bloque</h3>
     *
     * <p>Con el «Ninguna» de la 112 el bloque entero es el ámbito correcto, y eso es lo
     * que se escribió primero. Pero hay descartes que solo excluyen a una parte de su
     * bloque, y entonces el ámbito del bloque no es que sea impreciso: es que señala
     * contradicciones que no existen.</p>
     *
     * <p>El caso que lo obligó es la tabla de la 57, seis productos de tabaco por nueve
     * lugares de compra. Cada <b>renglón</b> empieza con «Yo no compré este producto»,
     * y excluye a los ocho lugares <em>de su renglón</em>. Con el ámbito del bloque,
     * quien no compró cigarros pero sí compró electrónicos en una gasolinera aparecería
     * contradiciéndose, y no se contradice en nada. Lo mismo pasa en la 145, que mete
     * tres listas distintas —partes del cuerpo, causa de la lesión y quién la trató— en
     * un solo bloque.</p>
     *
     * <p>Así que {@code comparaCon} admite las dos formas: el nombre de un bloque, o
     * varias preguntas separadas por {@code |}, que es el separador que el catálogo ya
     * usa para las listas de valores.</p>
     */
    private static Collection<String> aQuienExcluye(ReglaValidacion regla,
                                                    Map<String, VariableCuestionario> variables) {
        String ambito = regla.getComparaCon();
        if (ambito == null || ambito.isBlank()) return List.of();

        if (ambito.indexOf('|') >= 0) {
            List<String> codigos = new ArrayList<>();
            for (String c : ambito.split("\\|")) {
                String limpio = c.trim();
                if (!limpio.isEmpty()) codigos.add(limpio);
            }
            return codigos;
        }

        List<String> delBloque = new ArrayList<>();
        for (Map.Entry<String, VariableCuestionario> e : variables.entrySet()) {
            VariableCuestionario v = e.getValue();
            if (v.getSeccion() != null
                    && v.getSeccion().getNombre().equalsIgnoreCase(ambito)) {
                delBloque.add(e.getKey());
            }
        }
        // Un ambito que no es ningun bloque pero si es una pregunta se entiende como esa
        // sola pregunta. Sin esto, una regla que excluye a una unica hermana —escrita sin
        // la barra, porque con una no hace falta— no se disparaba nunca y nada lo decia.
        if (delBloque.isEmpty() && variables.containsKey(ambito)) return List.of(ambito);
        return delBloque;
    }

    private static Aviso requiereTexto(ReglaValidacion regla, Map<String, String> respuestas) {
        String contestado = respuestas.get(regla.getPregunta());
        boolean dispara = CUALQUIERA.equals(regla.getValor())
                ? tieneValor(contestado)
                : coincide(contestado, regla.getValor());
        if (!dispara) return null;
        return tieneValor(respuestas.get(regla.getComparaCon())) ? null : aviso(regla);
    }

    /**
     * El sexo sale de la respuesta del propio cuestionario, no del expediente: así la
     * regla se puede comprobar con lo que trae el archivo. Si no está contestado, no
     * se dice nada.
     */
    private static Aviso soloSexo(ReglaValidacion regla, Map<String, String> respuestas,
                                  Map<String, VariableCuestionario> variables) {
        String sexo = respuestas.get(regla.getComparaCon());
        if (!tieneValor(sexo)) return null;

        // En el cuestionario general, P3: 1 = Mujer, 2 = Hombre.
        boolean esMujer = "1".equals(sexo.trim());
        boolean esDeMujeres = "MUJERES".equalsIgnoreCase(regla.getValor());
        if (esMujer == esDeMujeres) return null;   // le corresponde

        // No le corresponde: solo importa si de verdad contestó algo.
        for (Map.Entry<String, VariableCuestionario> e : variables.entrySet()) {
            VariableCuestionario v = e.getValue();
            boolean suya = e.getKey().equals(regla.getPregunta())
                    || (v.getSeccion() != null
                        && v.getSeccion().getNombre().equalsIgnoreCase(regla.getPregunta()));
            if (suya && tieneValor(respuestas.get(e.getKey()))) return aviso(regla);
        }
        return null;
    }

    private static Aviso fechaNoFutura(ReglaValidacion regla, Map<String, String> respuestas) {
        String valor = respuestas.get(regla.getPregunta());
        if (!tieneValor(valor)) return null;
        try {
            // El mismo conversor que la carga, y por la misma razón: con
            // LocalDate.parse —solo ISO— una fecha escrita 13-06-2030 no se leía, así
            // que la comprobación se callaba justo en el caso que tiene que cazar.
            LocalDate fecha = NormalizadorFecha
                    .parsear(valor.trim(), NormalizadorFecha.Orden.DIA_MES).toLocalDate();
            return fecha.isAfter(LocalDate.now()) ? aviso(regla) : null;
        } catch (RuntimeException noEsUnaFecha) {
            // Que no se pueda leer como fecha es problema de otra comprobación, no de
            // esta: aquí solo se mira si está adelante de hoy.
            return null;
        }
    }

    private static Aviso noMayorQue(ReglaValidacion regla, Map<String, String> respuestas,
                                    Integer edad) {
        Double valor = numero(respuestas.get(regla.getPregunta()));
        if (valor == null) return null;

        Double tope = "EDAD".equalsIgnoreCase(regla.getComparaCon())
                ? (edad == null ? null : edad.doubleValue())
                : numero(respuestas.get(regla.getComparaCon()));
        if (tope == null) return null;

        return valor > tope ? aviso(regla) : null;
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private static Aviso aviso(ReglaValidacion regla) {
        return new Aviso(regla.getPregunta(), regla.getSeveridad(), regla.getMensaje());
    }

    private static boolean tieneValor(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean coincide(String contestado, String esperado) {
        return tieneValor(contestado) && contestado.trim().equals(
                esperado == null ? null : esperado.trim());
    }

    private static Double numero(String s) {
        if (!tieneValor(s)) return null;
        try {
            return Double.valueOf(s.trim().replace(',', '.'));
        } catch (NumberFormatException noEsUnNumero) {
            return null;
        }
    }

    /** Si una variable se contesta escribiendo, para saber dónde mirar el texto. */
    static boolean esTexto(VariableCuestionario v) {
        return v != null && v.getTipo() == TipoVariable.TEXTO;
    }
}
