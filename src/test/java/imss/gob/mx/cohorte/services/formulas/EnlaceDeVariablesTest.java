package imss.gob.mx.cohorte.services.formulas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que el dato llegue al cálculo en la unidad que la fórmula pidió.
 *
 * <p>Aquí se cierra el hueco que motivó todo el diseño: el dato está guardado en la
 * unidad en que se capturó, la fórmula lo necesita en otra, y quien la escribió eligió
 * cuál. La conversión ocurre en este punto, una sola vez y antes de calcular.</p>
 */
class EnlaceDeVariablesTest {

    /** Lo que devolvería el catálogo para cada clave, tal como está guardado. */
    private static final Map<String, Magnitud> GUARDADO = Map.of(
            "estudio.7.param.85",  Magnitud.de(new BigDecimal("165"), Unidad.de("cm")),
            "estudio.7.param.92",  Magnitud.de(new BigDecimal("68"), Unidad.de("kg")),
            "estudio.7.param.119", Magnitud.de(new BigDecimal("85"), Unidad.de("cm")),
            "examen.6.valor",      Magnitud.de(new BigDecimal("4200"), Unidad.de("mg/dL")));

    private static final Function<String, Magnitud> CATALOGO = GUARDADO::get;

    private static String calcular(String formula, List<VariableFormula> variables) {
        return Evaluador.evaluar(AnalizadorFormula.analizar(formula),
                EnlaceDeVariables.para(variables, CATALOGO)).texto(2);
    }

    @Test
    @DisplayName("la estatura entra en metros aunque esté guardada en centímetros")
    void seConvierteALaUnidadElegida() {
        List<VariableFormula> variables = List.of(
                VariableFormula.de("peso", "estudio.7.param.92", "kg"),
                VariableFormula.de("estatura", "estudio.7.param.85", "m"));

        assertThat(calcular("peso / estatura²", variables)).isEqualTo("24.98");
    }

    @Test
    @DisplayName("sin unidad elegida el dato entra tal como está guardado")
    void sinConversion() {
        List<VariableFormula> variables = List.of(
                VariableFormula.de("peso", "estudio.7.param.92"),
                VariableFormula.de("estatura", "estudio.7.param.85"));

        // Con la estatura en centímetros, la misma fórmula da 0.0025. Es el resultado
        // correcto de lo que se pidió, y por eso la unidad se elige a la vista.
        assertThat(calcular("peso / estatura²", variables)).isEqualTo("0.00");
    }

    @Test
    @DisplayName("la albúmina pasa de mg/dL a g/dL, que es como la imprime el reporte")
    void concentracionesDelLaboratorio() {
        List<VariableFormula> variables = List.of(
                VariableFormula.de("albumina", "examen.6.valor", "g/dL"));

        assertThat(calcular("albumina", variables)).isEqualTo("4.20");
    }

    @Test
    @DisplayName("una conversión imposible deja la celda vacía, no un número equivocado")
    void conversionImposible() {
        List<VariableFormula> variables = List.of(
                VariableFormula.de("peso", "estudio.7.param.92", "cm"));

        assertThat(calcular("peso * 2", variables)).isEmpty();
    }

    @Test
    @DisplayName("una clave que el catálogo no contesta es un dato que falta")
    void claveSinDato() {
        List<VariableFormula> variables = List.of(
                VariableFormula.de("vo2", "estudio.4.param.999", "LPM"));

        assertThat(calcular("vo2 + 1", variables)).isEmpty();
    }

    @Test
    @DisplayName("un nombre que la fórmula usa sin declarar se contesta como ausente")
    void nombreSinDeclarar() {
        // Guardar así lo impide el validador; el enlace no revienta, deja el hueco.
        List<VariableFormula> variables = List.of(
                VariableFormula.de("peso", "estudio.7.param.92", "kg"));

        assertThat(calcular("peso + desconocida", variables)).isEmpty();
    }
}
