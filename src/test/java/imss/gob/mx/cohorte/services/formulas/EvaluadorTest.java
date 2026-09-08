package imss.gob.mx.cohorte.services.formulas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El cálculo: qué sale, qué unidad lleva y qué pasa cuando falta un dato.
 *
 * <p>La parte más importante de todas es la ausencia. Un dato que no está tiene que
 * dejar la celda en blanco y no un cero, porque un cero impreso en un documento
 * clínico se lee como una medición.</p>
 */
class EvaluadorTest {

    private static Magnitud calcular(String formula, Map<String, Magnitud> variables) {
        return Evaluador.evaluar(AnalizadorFormula.analizar(formula), variables::get);
    }

    private static Magnitud calcular(String formula) {
        return calcular(formula, Map.of());
    }

    private static Magnitud numero(String valor, String unidad) {
        return Magnitud.de(new BigDecimal(valor), Unidad.de(unidad));
    }

    // ── Ausencia ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("un dato que falta deja la celda en blanco, no en cero")
    void laAusenciaSeContagia() {
        Map<String, Magnitud> soloEstatura = Map.of("estatura", numero("1.65", "m"));

        // El peso no está: el índice de masa corporal no es cero, es incalculable.
        Magnitud imc = calcular("peso / estatura²", soloEstatura);

        assertThat(imc.ausente()).isTrue();
        assertThat(imc.texto(2)).isEmpty();
    }

    @Test
    @DisplayName("una variable que nadie sabe contestar es un dato que falta")
    void variableDesconocida() {
        assertThat(calcular("noExiste + 1").ausente()).isTrue();
    }

    @Test
    @DisplayName("dividir entre cero deja en blanco en vez de reventar")
    void divisionEntreCero() {
        Map<String, Magnitud> vars = Map.of(
                "cintura", numero("85", "cm"),
                "cadera", numero("0", "cm"));

        assertThat(calcular("cintura / cadera", vars).ausente()).isTrue();
    }

    // ── Unidades ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sumar cantidades de la misma unidad la conserva")
    void sumarConservaLaUnidad() {
        Map<String, Magnitud> vars = Map.of(
                "a", numero("10", "cm"),
                "b", numero("5", "cm"));

        Magnitud r = calcular("a + b", vars);
        assertThat(r.texto(null)).isEqualTo("15");
        assertThat(r.unidad().nombre()).isEqualTo("cm");
    }

    @Test
    @DisplayName("mezclar unidades se permite, pero el resultado no hereda ninguna")
    void mezclarUnidadesNoSeBloquea() {
        // Se decidió expresamente que la herramienta no impone criterio: si alguien
        // quiere sumar centímetros con kilos, lo hace. Lo que no se hace es etiquetar
        // el resultado como si fuera una de las dos.
        Map<String, Magnitud> vars = Map.of(
                "largo", numero("10", "cm"),
                "masa", numero("5", "kg"));

        Magnitud r = calcular("largo + masa", vars);
        assertThat(r.ausente()).isFalse();
        assertThat(r.texto(null)).isEqualTo("15");
        assertThat(r.unidad().nombre()).isEmpty();
    }

    @Test
    @DisplayName("multiplicar por un número pelado no cambia la unidad")
    void escalarConservaLaUnidad() {
        Magnitud r = calcular("peso * 2", Map.of("peso", numero("70", "kg")));
        assertThat(r.unidad().nombre()).isEqualTo("kg");
    }

    @Test
    @DisplayName("al dividir dos unidades el resultado ya no tiene nombre")
    void dividirDejaSinUnidad() {
        Map<String, Magnitud> vars = Map.of(
                "peso", numero("68", "kg"),
                "estatura", numero("1.65", "m"));

        // kg entre metros al cuadrado no está en el catálogo de unidades. La que se
        // imprime es la que declaró quien escribió la fórmula.
        assertThat(calcular("peso / estatura²", vars).unidad().nombre()).isEmpty();
    }

    // ── Funciones ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("«si» elige según la condición")
    void condicional() {
        Map<String, Magnitud> mujer = Map.of("sexo", numero("1", ""));
        Map<String, Magnitud> hombre = Map.of("sexo", numero("0", ""));

        assertThat(calcular("si(sexo = 1, 100, 200)", mujer).texto(null)).isEqualTo("100");
        assertThat(calcular("si(sexo = 1, 100, 200)", hombre).texto(null)).isEqualTo("200");
    }

    @Test
    @DisplayName("«si» no calcula la rama que no se toma")
    void laRamaDescartadaNoEstorba() {
        // La rama de hombres usa un dato que esta participante no tiene. Si se
        // evaluaran las dos, el resultado saldría ausente sin ninguna razón.
        Map<String, Magnitud> vars = Map.of("sexo", numero("1", ""));

        assertThat(calcular("si(sexo = 1, 42, datoQueNoExiste)", vars).texto(null))
                .isEqualTo("42");
    }

    @Test
    @DisplayName("las comparaciones valen uno o cero")
    void comparaciones() {
        Map<String, Magnitud> vars = Map.of("x", numero("10", ""));

        assertThat(calcular("x > 5", vars).texto(null)).isEqualTo("1");
        assertThat(calcular("x < 5", vars).texto(null)).isEqualTo("0");
        assertThat(calcular("x >= 10", vars).texto(null)).isEqualTo("1");
        assertThat(calcular("x <> 10", vars).texto(null)).isEqualTo("0");
    }

    @Test
    @DisplayName("mínimo, máximo, promedio, valor absoluto y raíz")
    void funcionesDeVarios() {
        assertThat(calcular("min(3, 7, 5)").texto(null)).isEqualTo("3");
        assertThat(calcular("max(3, 7, 5)").texto(null)).isEqualTo("7");
        assertThat(calcular("promedio(2, 4, 6)").texto(null)).isEqualTo("4");
        assertThat(calcular("abs(0 - 8)").texto(null)).isEqualTo("8");
        assertThat(calcular("raiz(9)").texto(2)).isEqualTo("3.00");
    }

    @Test
    @DisplayName("si falta uno de los valores, el mínimo del conjunto no se sabe")
    void elMinimoNoIgnoraLosHuecos() {
        // Devolver el mínimo de los que sí están sería otra cosa, y en la hoja no habría
        // forma de distinguirla.
        assertThat(calcular("min(3, faltante, 5)").ausente()).isTrue();
    }

    @Test
    @DisplayName("redondear dentro de la fórmula recorta el valor, no solo lo que se ve")
    void redondeoExplicito() {
        assertThat(calcular("redondear(24.977, 2)").texto(null)).isEqualTo("24.98");

        // Se pide con más decimales de los que quedaron para comprobar que el recorte
        // ocurrió de verdad: si «redondear» no hiciera nada, aquí saldría 24.950.
        assertThat(calcular("redondear(24.95, 1)").texto(3)).isEqualTo("25.000");
        assertThat(calcular("24.95").texto(3)).isEqualTo("24.950");
    }

    // ── Aritmética exacta ────────────────────────────────────────────────────

    @Test
    @DisplayName("la aritmética es decimal exacta, no de punto flotante")
    void sinErrorDePuntoFlotante() {
        // En punto flotante esto da 0.30000000000000004.
        assertThat(calcular("0.1 + 0.2").texto(null)).isEqualTo("0.3");
    }

    @Test
    @DisplayName("los exponentes desmedidos no se calculan")
    void exponenteAcotado() {
        // Elevar a un número enorme come memoria y tiempo, y estas fórmulas vienen de
        // la base de datos.
        assertThat(calcular("2 ^ 999999").ausente()).isTrue();
        assertThat(calcular("2 ^ 10").texto(null)).isEqualTo("1024");
    }

    @Test
    @DisplayName("la raíz de un número negativo no existe y sale en blanco")
    void raizNegativa() {
        assertThat(calcular("raiz(0 - 4)").ausente()).isTrue();
    }
}
