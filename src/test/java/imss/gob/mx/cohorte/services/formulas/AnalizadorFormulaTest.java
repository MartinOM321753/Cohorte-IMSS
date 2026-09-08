package imss.gob.mx.cohorte.services.formulas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Que la fórmula escrita se lea como quien la escribió esperaba.
 *
 * <p>Dos cosas se prueban aquí. La primera es la precedencia: si «18.5 × talla ^ 2» se
 * leyera de izquierda a derecha daría un número distinto, y nada lo delataría. La
 * segunda son los mensajes de error, que los lee quien está armando el reporte y no un
 * programador.</p>
 */
class AnalizadorFormulaTest {

    /** Atajo para calcular sin variables. */
    private static String calcular(String formula) {
        return Evaluador.evaluar(AnalizadorFormula.analizar(formula), n -> null).texto(null);
    }

    @Test
    @DisplayName("la potencia va antes que la multiplicación")
    void precedenciaDeLaPotencia() {
        // Leído de izquierda a derecha daría (18.5 × 2)² = 1369.
        assertThat(calcular("18.5 * 2 ^ 2")).isEqualTo("74");
    }

    @Test
    @DisplayName("multiplicar va antes que sumar, y el paréntesis manda sobre todo")
    void precedenciaHabitual() {
        assertThat(calcular("2 + 3 * 4")).isEqualTo("14");
        assertThat(calcular("(2 + 3) * 4")).isEqualTo("20");
    }

    @Test
    @DisplayName("la potencia asocia a la derecha")
    void potenciaALaDerecha() {
        // 2^(3^2) = 512, no (2^3)^2 = 64.
        assertThat(calcular("2 ^ 3 ^ 2")).isEqualTo("512");
    }

    @Test
    @DisplayName("el cuadrado se puede escribir con el superíndice")
    void superindice() {
        // Es como viene escrito en el documento del que salen estas fórmulas.
        assertThat(calcular("3² + 1")).isEqualTo("10");
        assertThat(calcular("2³")).isEqualTo("8");
    }

    @Test
    @DisplayName("los operadores valen escritos con el teclado o con el signo bonito")
    void operadoresEnSusDosFormas() {
        assertThat(calcular("6 * 2")).isEqualTo(calcular("6 × 2"));
        assertThat(calcular("6 / 2")).isEqualTo(calcular("6 ÷ 2"));
        // El menos tipográfico, que es el que pega un procesador de texto.
        assertThat(calcular("6 − 2")).isEqualTo("4");
    }

    @Test
    @DisplayName("un nombre puede ser la clave de un campo del catálogo")
    void nombresConPuntosYDigitos() {
        Expresion e = AnalizadorFormula.analizar("estudio.7.param.85 + 1");
        assertThat(e).isInstanceOf(Expresion.Operacion.class);

        Expresion izquierda = ((Expresion.Operacion) e).izquierda();
        assertThat(izquierda).isEqualTo(new Expresion.Variable("estudio.7.param.85"));
    }

    @Test
    @DisplayName("el paréntesis sin cerrar se avisa señalando dónde")
    void parentesisSinCerrar() {
        assertThatThrownBy(() -> AnalizadorFormula.analizar("(2 + 3"))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("Falta cerrar un paréntesis");
    }

    @Test
    @DisplayName("una función que no existe dice cuáles sí")
    void funcionInexistente() {
        assertThatThrownBy(() -> AnalizadorFormula.analizar("logaritmo(8)"))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("No existe la función «logaritmo»")
                .hasMessageContaining("redondear");
    }

    @Test
    @DisplayName("una función mal usada se detecta al escribir, no al emitir")
    void argumentosQueNoCuadran() {
        assertThatThrownBy(() -> AnalizadorFormula.analizar("redondear(3.7)"))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("cuántos decimales");

        assertThatThrownBy(() -> AnalizadorFormula.analizar("si(1 > 0, 5)"))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("tres");
    }

    @Test
    @DisplayName("lo que sobra al final se señala en vez de ignorarse")
    void textoDeMas() {
        assertThatThrownBy(() -> AnalizadorFormula.analizar("2 + 3 4"))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("Sobra");
    }

    @Test
    @DisplayName("una fórmula vacía no es una fórmula")
    void vacia() {
        assertThatThrownBy(() -> AnalizadorFormula.analizar("   "))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("vacía");
    }

    @Test
    @DisplayName("no se admiten fórmulas desmedidas")
    void losLimitesSeAplican() {
        // Sin tope, una expresión así se guarda y después tumba la emisión del reporte
        // de cada participante.
        String demasiadoLarga = "1+".repeat(AnalizadorFormula.LARGO_MAXIMO) + "1";
        assertThatThrownBy(() -> AnalizadorFormula.analizar(demasiadoLarga))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("demasiado larga");

        String muyAnidada = "(".repeat(80) + "1" + ")".repeat(80);
        assertThatThrownBy(() -> AnalizadorFormula.analizar(muyAnidada))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("paréntesis anidados");
    }

    @Test
    @DisplayName("no hay forma de invocar código desde una fórmula")
    void nadaDeCodigo() {
        // Estas expresiones llegan de la base y las escriben usuarios. Un evaluador de
        // propósito general las ejecutaría; este ni siquiera sabe leerlas.
        for (String intento : new String[]{
                "T(java.lang.Runtime).getRuntime()",
                "new java.io.File('x')",
                "''.getClass()",
                "#{7*7}"}) {
            assertThatThrownBy(() -> AnalizadorFormula.analizar(intento))
                    .as("no debería poder leerse: %s", intento)
                    .isInstanceOf(ErrorDeFormula.class);
        }
    }
}
