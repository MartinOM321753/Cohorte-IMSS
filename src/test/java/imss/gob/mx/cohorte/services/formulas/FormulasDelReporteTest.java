package imss.gob.mx.cohorte.services.formulas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El criterio de aceptación del motor: las fórmulas de un reporte real.
 *
 * <p>Estas son, literalmente, las que trae el «Reporte de salud HWC» que dio origen al
 * módulo. No están aquí para dejarlas programadas —las escribirá quien arme el reporte
 * desde la pantalla— sino para comprobar que <b>se pueden expresar</b>. Si alguna no
 * cupiera en este motor, el diseño estaría corto, y vale más saberlo ahora.</p>
 *
 * <p>Cubren los cuatro mecanismos que el documento usa y que el motor tiene que
 * sostener: un valor derivado de otros dos, un rango cuyos límites se calculan, una
 * ecuación con rama por sexo y una razón entre dos medidas.</p>
 */
class FormulasDelReporteTest {

    private Map<String, Magnitud> participante;

    private void dato(String nombre, String valor, String unidad) {
        if (participante == null) participante = new HashMap<>();
        participante.put(nombre, Magnitud.de(new BigDecimal(valor), Unidad.de(unidad)));
    }

    private String calcular(String formula, int decimales) {
        return Evaluador.evaluar(AnalizadorFormula.analizar(formula), participante::get)
                .texto(decimales);
    }

    // ── Valores derivados ────────────────────────────────────────────────────

    @Test
    @DisplayName("índice de masa corporal")
    void indiceDeMasaCorporal() {
        dato("peso", "68", "kg");
        dato("estatura", "1.65", "m");

        assertThat(calcular("peso / estatura²", 2)).isEqualTo("24.98");
    }

    @Test
    @DisplayName("índice cintura/cadera, con su referencia por sexo")
    void indiceCinturaCadera() {
        dato("cintura", "85", "cm");
        dato("cadera", "100", "cm");
        dato("sexo", "1", "");   // 1 = mujer

        assertThat(calcular("cintura / cadera", 2)).isEqualTo("0.85");

        // El documento pide ♀ <0.85 y ♂ <0.90: el límite también sale de una fórmula.
        assertThat(calcular("si(sexo = 1, 0.85, 0.90)", 2)).isEqualTo("0.85");
    }

    // ── Rango calculado ──────────────────────────────────────────────────────

    @Test
    @DisplayName("peso ideal: los dos límites del rango dependen de la estatura")
    void pesoIdeal() {
        dato("talla", "1.65", "m");

        // «18.5 × talla² — 24.9 × talla² kg», tal cual lo imprime el documento.
        assertThat(calcular("18.5 * talla²", 2)).isEqualTo("50.37");
        assertThat(calcular("24.9 * talla²", 2)).isEqualTo("67.79");
    }

    // ── Ecuaciones con rama por sexo ─────────────────────────────────────────

    @Test
    @DisplayName("distancia esperada en la caminata de 6 minutos, en mujeres")
    void caminataSeisMinutosMujer() {
        dato("estatura", "160", "cm");
        dato("edad", "45", "años");
        dato("peso", "62", "kg");

        assertThat(calcular("2.11 * estatura - 5.78 * edad - 2.29 * peso + 667", 2))
                .isEqualTo("602.52");
    }

    @Test
    @DisplayName("distancia esperada en la caminata de 6 minutos, en hombres")
    void caminataSeisMinutosHombre() {
        dato("estatura", "175", "cm");
        dato("edad", "50", "años");
        dato("peso", "80", "kg");

        assertThat(calcular("7.57 * estatura - 5.02 * edad - 1.76 * peso - 309", 2))
                .isEqualTo("623.95");
    }

    @Test
    @DisplayName("una sola fórmula resuelve las dos ramas del documento")
    void lasDosRamasEnUnaFormula() {
        // Así es como quedaría escrita en el editor: una fórmula por fila del reporte,
        // no una por sexo.
        String distanciaEsperada = """
                si(sexo = 1,
                   2.11 * estatura - 5.78 * edad - 2.29 * peso + 667,
                   7.57 * estatura - 5.02 * edad - 1.76 * peso - 309)""";

        dato("estatura", "160", "cm");
        dato("edad", "45", "años");
        dato("peso", "62", "kg");
        dato("sexo", "1", "");
        assertThat(calcular(distanciaEsperada, 2)).isEqualTo("602.52");

        dato("estatura", "175", "cm");
        dato("edad", "50", "años");
        dato("peso", "80", "kg");
        dato("sexo", "0", "");
        assertThat(calcular(distanciaEsperada, 2)).isEqualTo("623.95");
    }

    @Test
    @DisplayName("VO₂ máximo estimado, por sexo")
    void consumoMaximoDeOxigeno() {
        String vo2 = "si(sexo = 1, 65.81 - 0.1847 * fcRecuperacion, 111.33 - 0.42 * fcRecuperacion)";

        dato("fcRecuperacion", "150", "LPM");
        dato("sexo", "1", "");
        assertThat(calcular(vo2, 3)).isEqualTo("38.105");

        dato("sexo", "0", "");
        assertThat(calcular(vo2, 2)).isEqualTo("48.33");
    }

    // ── Lo que pasa con un participante incompleto ───────────────────────────

    @Test
    @DisplayName("al participante sin peso registrado se le deja la celda vacía")
    void participanteIncompleto() {
        dato("estatura", "160", "cm");
        dato("edad", "45", "años");
        dato("sexo", "1", "");
        // Sin peso: la ecuación lo necesita.

        assertThat(calcular("2.11 * estatura - 5.78 * edad - 2.29 * peso + 667", 2)).isEmpty();
        assertThat(calcular("peso / estatura²", 2)).isEmpty();
    }

    @Test
    @DisplayName("la estatura en la unidad equivocada es el error que nada delata")
    void launidadImportaMasQueLaFormula() {
        // La misma fórmula, bien escrita, con la estatura tal como está guardada en el
        // catálogo —centímetros— y convertida a metros, que es lo que pide.
        Magnitud guardada = Magnitud.de(new BigDecimal("165"), Unidad.de("cm"));

        participante = new HashMap<>();
        participante.put("peso", Magnitud.de(new BigDecimal("68"), Unidad.de("kg")));

        participante.put("estatura", guardada);
        assertThat(calcular("peso / estatura²", 4)).isEqualTo("0.0025");

        participante.put("estatura", guardada.en(Unidad.de("m")));
        assertThat(calcular("peso / estatura²", 2)).isEqualTo("24.98");
    }
}
