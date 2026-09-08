package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.services.formulas.AnalizadorFormula;
import imss.gob.mx.cohorte.services.formulas.EnlaceDeVariables;
import imss.gob.mx.cohorte.services.formulas.ErrorDeFormula;
import imss.gob.mx.cohorte.services.formulas.Evaluador;
import imss.gob.mx.cohorte.services.formulas.Magnitud;
import imss.gob.mx.cohorte.services.formulas.Unidad;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Aplica una fórmula del catálogo a un participante.
 *
 * <p>Es el punto donde se juntan las tres piezas: el resolvedor saca los datos del
 * participante, el enlace los pasa a la unidad que la fórmula pidió, y el evaluador
 * calcula. Vive aquí y no dentro del maquetador porque el mismo cálculo lo van a
 * necesitar la emisión del reporte, la descarga de datos y la vista previa del editor;
 * metido en el maquetador solo serviría para imprimir.</p>
 *
 * <p><b>Nunca lanza.</b> Una fórmula rota deja la celda vacía y escribe una línea en la
 * bitácora. Que un reporte entero falle porque una celda de una plantilla vieja quedó
 * mal sería peor que el hueco, y es el mismo criterio que ya sigue el resolvedor de
 * campos con las claves que no sabe resolver.</p>
 */
@Service
@AllArgsConstructor
@Slf4j
public class CalculadoraFormulas {

    private final FormulaReporteService formulaService;

    /**
     * El resultado como cantidad, para comparar contra un rango sin redondear antes.
     *
     * <p>De dónde salen los datos se recibe de fuera y no se toma de un colaborador
     * fijo. Es lo que evita que esta clase y el resolvedor de campos dependan la una de
     * la otra: el resolvedor necesita a la calculadora para imprimir una fórmula, y la
     * calculadora necesita al resolvedor para leer las variables. Pasando la función se
     * rompe el círculo sin trucos de inyección.</p>
     */
    public Magnitud calcular(FormulaReporte formula, ContextoReporte ctx,
                             java.util.function.Function<String, Magnitud> porClave) {
        if (formula == null || ctx == null) return Magnitud.sinDato();

        try {
            Magnitud resultado = Evaluador.evaluar(
                    AnalizadorFormula.analizar(formula.getExpresion()),
                    EnlaceDeVariables.para(formulaService.variablesDe(formula), porClave));

            if (resultado.ausente()) return resultado;

            // La unidad que se imprime es la que declaró quien escribió la fórmula. El
            // evaluador no puede nombrar la que sale de dividir kilos entre metros al
            // cuadrado, y tampoco le corresponde discutirla.
            Unidad declarada = formula.getUnidadSalida() == null
                    ? resultado.unidad()
                    : Unidad.de(formula.getUnidadSalida());
            return Magnitud.de(resultado.valor(), declarada);

        } catch (ErrorDeFormula mal) {
            log.warn("Fórmula «{}» (id {}) no se pudo leer al emitir: {}",
                    formula.getNombre(), formula.getId(), mal.getMessage());
            return Magnitud.sinDato();
        } catch (RuntimeException inesperado) {
            return sinDatoOPropagar(formula, inesperado, "calcular");
        }
    }

    /**
     * Decide si el fallo deja un hueco o tumba la emisión.
     *
     * <p>Una fórmula mal escrita deja la celda vacía: es un problema de esa celda y no
     * tiene por qué llevarse por delante el documento entero.</p>
     *
     * <p><b>Un fallo de la base de datos, no.</b> Cuando la capa de persistencia lanza
     * —un parámetro que ya no existe detrás de una referencia perezosa, por ejemplo—
     * deja la transacción marcada como «solo reversión», y ahí ya no hay vuelta atrás:
     * el reporte se sigue armando, se entrega, y al confirmar la transacción al final
     * revienta con un <i>UnexpectedRollbackException</i> que no menciona ni la fórmula
     * ni el dato. El error aparece a doscientos milisegundos y a diez capas del sitio
     * donde estaba el problema.</p>
     *
     * <p>Por eso se distingue: lo que es de la fórmula se traga con su aviso, y lo que
     * es de los datos se deja subir para que falle donde de verdad falló.</p>
     */
    private Magnitud sinDatoOPropagar(FormulaReporte formula, RuntimeException fallo, String momento) {
        if (fallo instanceof jakarta.persistence.PersistenceException
                || fallo instanceof org.springframework.dao.DataAccessException) {
            log.error("Fórmula «{}» (id {}): fallo de datos al {}. No se deja pasar, porque la "
                            + "transacción ya quedó marcada para reversión.",
                    formula.getNombre(), formula.getId(), momento);
            throw fallo;
        }
        // Con la traza completa, no solo el mensaje: cuando una fórmula falla al emitir
        // hay que poder ver de dónde salió sin tener que reproducirlo.
        log.error("Fórmula «{}» (id {}) falló al {}; la celda queda vacía",
                formula.getNombre(), formula.getId(), momento, fallo);
        return Magnitud.sinDato();
    }

    // ── La referencia ────────────────────────────────────────────────────────

    /** El límite inferior de la referencia, cuando la fórmula lo define. */
    public Magnitud minimo(FormulaReporte formula, ContextoReporte ctx,
                           java.util.function.Function<String, Magnitud> porClave) {
        return limite(formula, formula == null ? null : formula.getExpresionMinimo(), ctx, porClave);
    }

    public Magnitud maximo(FormulaReporte formula, ContextoReporte ctx,
                           java.util.function.Function<String, Magnitud> porClave) {
        return limite(formula, formula == null ? null : formula.getExpresionMaximo(), ctx, porClave);
    }

    /**
     * La referencia escrita, como se lee en la columna: «50.37 – 67.79», «&lt; 200».
     *
     * <p>Con un solo límite se escribe con su símbolo, porque el documento del que salen
     * estas fórmulas también lo hace así: hay filas con rango cerrado, filas con solo
     * techo y filas con solo piso.</p>
     */
    public String textoReferencia(FormulaReporte formula, ContextoReporte ctx,
                                  java.util.function.Function<String, Magnitud> porClave) {
        Integer decimales = formula == null ? null : formula.getDecimales();
        Magnitud min = minimo(formula, ctx, porClave);
        Magnitud max = maximo(formula, ctx, porClave);

        if (min.presente() && max.presente()) {
            return min.texto(decimales) + " – " + max.texto(decimales);
        }
        if (max.presente()) return "≤ " + max.texto(decimales);
        if (min.presente()) return "≥ " + min.texto(decimales);
        return "";
    }

    /**
     * Si el valor cae dentro de su referencia.
     *
     * <p>La comparación usa el valor <b>sin redondear</b>, no el que se imprime. Es la
     * diferencia entre clasificar un 24.95 como dentro o como fuera cuando el techo son
     * 24.9 décimas: redondear antes de comparar cambia el resultado clínico, no solo lo
     * que se ve.</p>
     *
     * <p>Los rangos cerrados incluyen sus dos extremos. Se decidió así porque el
     * documento escribe «70–99» sin más, y quien lo lee entiende que el 99 está dentro;
     * cuando el límite es excluyente, el documento lo dice con «&lt;».</p>
     *
     * <p>Sin valor o sin referencia devuelve cadena vacía: «no se sabe» no es «está
     * mal», y marcar un hallazgo falso en un documento clínico es peor que no marcar
     * nada.</p>
     */
    public String estado(FormulaReporte formula, ContextoReporte ctx,
                         java.util.function.Function<String, Magnitud> porClave) {
        Magnitud valor = calcular(formula, ctx, porClave);
        if (valor.ausente()) return "";

        Magnitud min = minimo(formula, ctx, porClave);
        Magnitud max = maximo(formula, ctx, porClave);
        if (min.ausente() && max.ausente()) return "";

        if (max.presente() && valor.valor().compareTo(max.valor()) > 0) return "Por arriba";
        if (min.presente() && valor.valor().compareTo(min.valor()) < 0) return "Por abajo";
        return "Dentro del rango";
    }

    private Magnitud limite(FormulaReporte formula, String expresion, ContextoReporte ctx,
                            java.util.function.Function<String, Magnitud> porClave) {
        if (formula == null || ctx == null || expresion == null || expresion.isBlank()) {
            return Magnitud.sinDato();
        }
        try {
            Magnitud resultado = Evaluador.evaluar(
                    AnalizadorFormula.analizar(expresion),
                    EnlaceDeVariables.para(formulaService.variablesDe(formula), porClave));
            if (resultado.ausente()) return resultado;

            Unidad declarada = formula.getUnidadSalida() == null
                    ? resultado.unidad()
                    : Unidad.de(formula.getUnidadSalida());
            return Magnitud.de(resultado.valor(), declarada);
        } catch (RuntimeException mal) {
            return sinDatoOPropagar(formula, mal, "calcular un límite de la referencia");
        }
    }

    /** El resultado ya listo para la celda, con los decimales que la fórmula declara. */
    public String texto(FormulaReporte formula, ContextoReporte ctx,
                        java.util.function.Function<String, Magnitud> porClave) {
        Magnitud resultado = calcular(formula, ctx, porClave);
        return resultado.texto(formula == null ? null : formula.getDecimales());
    }

    /** Con la unidad detrás: «24.98 kg/m²». */
    public String textoConUnidad(FormulaReporte formula, ContextoReporte ctx,
                                 java.util.function.Function<String, Magnitud> porClave) {
        Magnitud resultado = calcular(formula, ctx, porClave);
        return resultado.textoConUnidad(formula == null ? null : formula.getDecimales());
    }
}
