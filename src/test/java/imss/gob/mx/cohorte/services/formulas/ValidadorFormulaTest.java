package imss.gob.mx.cohorte.services.formulas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Qué impide guardar una fórmula y qué solo se advierte.
 *
 * <p>La separación entre los dos niveles es una decisión de producto, no un detalle: se
 * acordó que la herramienta ofrece las operaciones y que el criterio de qué tiene
 * sentido es de quien arma el reporte. Solo se detiene lo que no tiene arreglo.</p>
 */
class ValidadorFormulaTest {

    private static final VariableFormula PESO     = VariableFormula.de("peso", "estudio.7.param.92", "kg");
    private static final VariableFormula ESTATURA = VariableFormula.de("estatura", "estudio.7.param.85", "m");
    private static final VariableFormula CINTURA  = VariableFormula.de("cintura", "estudio.7.param.119", "cm");
    private static final VariableFormula CADERA   = VariableFormula.de("cadera", "estudio.7.param.120", "cm");

    private static ValidadorFormula.Revision revisar(String formula, List<VariableFormula> vars) {
        return ValidadorFormula.revisar(formula, vars, null);
    }

    // ── Lo que impide ────────────────────────────────────────────────────────

    @Test
    @DisplayName("una fórmula que no se puede leer no se guarda")
    void sintaxisRota() {
        ValidadorFormula.Revision r = revisar("peso / (estatura", List.of(PESO, ESTATURA));

        assertThat(r.sePuedeGuardar()).isFalse();
        assertThat(r.avisos()).singleElement()
                .extracting(ValidadorFormula.Aviso::mensaje).asString()
                .contains("Falta cerrar un paréntesis");
    }

    @Test
    @DisplayName("una variable que nadie declaró no se guarda")
    void variableSinDeclarar() {
        ValidadorFormula.Revision r = revisar("peso / talla²", List.of(PESO));

        assertThat(r.sePuedeGuardar()).isFalse();
        assertThat(r.deNivel(ValidadorFormula.Nivel.IMPIDE))
                .extracting(ValidadorFormula.Aviso::mensaje).asString()
                .contains("«talla»");
    }

    @Test
    @DisplayName("prometer una unidad que la operación no produce no se guarda")
    void unidadDeSalidaQueNoCuadra() {
        // Sumar dos cantidades en centímetros da centímetros, no kilogramos. Aquí sí se
        // puede afirmar, y por eso se detiene.
        ValidadorFormula.Revision r =
                ValidadorFormula.revisar("cintura + cadera", List.of(CINTURA, CADERA), Unidad.de("kg"));

        assertThat(r.sePuedeGuardar()).isFalse();
        assertThat(r.deNivel(ValidadorFormula.Nivel.IMPIDE))
                .extracting(ValidadorFormula.Aviso::mensaje).asString()
                .contains("declara que sale en kg")
                .contains("da cm");
    }

    @Test
    @DisplayName("cuando la unidad del resultado no se puede afirmar, no se estorba")
    void unidadDerivadaNoSeDiscute() {
        // «peso ÷ estatura²» da kilogramos entre metros al cuadrado, que no está en
        // ningún catálogo. Quien escribió la fórmula declaró kg/m² y se le cree.
        ValidadorFormula.Revision r = ValidadorFormula.revisar(
                "peso / estatura²", List.of(PESO, ESTATURA), Unidad.de("kg/m²"));

        assertThat(r.sePuedeGuardar()).isTrue();
        assertThat(r.avisos()).isEmpty();
    }

    // ── Lo que solo advierte ─────────────────────────────────────────────────

    @Test
    @DisplayName("mezclar unidades avisa pero deja guardar")
    void mezclarUnidadesSoloAvisa() {
        ValidadorFormula.Revision r = revisar("peso + estatura", List.of(PESO, ESTATURA));

        assertThat(r.sePuedeGuardar())
                .as("la herramienta no impone criterio: es decisión de quien arma el reporte")
                .isTrue();
        assertThat(r.deNivel(ValidadorFormula.Nivel.ADVIERTE))
                .extracting(ValidadorFormula.Aviso::mensaje).asString()
                .contains("Se suma kg con m");
    }

    @Test
    @DisplayName("una unidad que no se reconoce avisa de lo que se pierde")
    void unidadNoReconocida() {
        VariableFormula rara = VariableFormula.de("lectura", "estudio.9.param.200", "Según equipo");
        ValidadorFormula.Revision r = revisar("lectura * 2", List.of(rara));

        assertThat(r.sePuedeGuardar()).isTrue();
        assertThat(r.deNivel(ValidadorFormula.Nivel.ADVIERTE))
                .extracting(ValidadorFormula.Aviso::mensaje).asString()
                .contains("no se reconoce")
                .contains("no se puede convertir");
    }

    // ── Lo que pasa limpio ───────────────────────────────────────────────────

    @Test
    @DisplayName("las fórmulas del reporte de salud pasan sin un solo aviso")
    void lasDelReporteReal() {
        assertThat(ValidadorFormula.revisar("cintura / cadera",
                List.of(CINTURA, CADERA), null).avisos()).isEmpty();

        VariableFormula edad = VariableFormula.de("edad", "participante.edad", "años");
        VariableFormula estaturaCm = VariableFormula.de("estatura", "estudio.7.param.85", "cm");
        VariableFormula sexo = VariableFormula.de("sexo", "participante.sexo", null);

        ValidadorFormula.Revision r = revisar("""
                si(sexo = 1,
                   2.11 * estatura - 5.78 * edad - 2.29 * peso + 667,
                   7.57 * estatura - 5.02 * edad - 1.76 * peso - 309)""",
                List.of(sexo, estaturaCm, edad, PESO));

        assertThat(r.sePuedeGuardar()).isTrue();
        assertThat(r.deNivel(ValidadorFormula.Nivel.IMPIDE)).isEmpty();
    }

    @Test
    @DisplayName("se pueden listar las variables que una fórmula usa")
    void variablesUsadas() {
        Expresion e = AnalizadorFormula.analizar("si(sexo = 1, peso / estatura², peso * 2)");

        assertThat(ValidadorFormula.nombresUsados(e))
                .containsExactly("sexo", "peso", "estatura");
    }
}
