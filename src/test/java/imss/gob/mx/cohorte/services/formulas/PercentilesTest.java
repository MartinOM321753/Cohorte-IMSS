package imss.gob.mx.cohorte.services.formulas;

import imss.gob.mx.cohorte.services.referencias.TablaReferencia;
import imss.gob.mx.cohorte.services.referencias.TablasReferencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La tabla de percentiles de dinamometría y las funciones que la usan.
 *
 * <p>Los valores esperados salen de la hoja de origen, no de la propia tabla: si alguien
 * corrige un dato del JSON, estas pruebas obligan a decidir si el cambio es intencional.</p>
 */
class PercentilesTest {

    private static final String PERCENTIL =
            "percentil(fuerza, p5, p10, p20, p30, p40, p50, p60, p70, p80, p90, p95)";
    private static final String BANDA =
            "bandaPercentil(fuerza, p5, p10, p20, p30, p40, p50, p60, p70, p80, p90, p95)";

    private final TablasReferencia tablas = TablasReferencia.estandar();

    /** Hombre de 22 años: el primer renglón de la hoja. */
    private Map<String, Magnitud> hombreDe22(String fuerza) {
        Map<String, Magnitud> datos = new HashMap<>();
        datos.put("fuerza", Magnitud.de(new BigDecimal(fuerza), Unidad.de("kg")));
        for (int p : Percentiles.PERCENTILES) {
            datos.put("p" + p, tablas.corte(TablasReferencia.clave("dinamometria", p), false, 22));
        }
        return datos;
    }

    private Magnitud evaluar(String formula, Map<String, Magnitud> datos) {
        return Evaluador.evaluar(AnalizadorFormula.analizar(formula), datos::get);
    }

    // ── La tabla ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("el archivo de dinamometría pasa su propia validación")
    void laTablaEsValida() {
        TablaReferencia t = tablas.tabla("dinamometria").orElseThrow();

        assertThat(t.problemas()).isEmpty();
        assertThat(t.filas()).hasSize(34);
        assertThat(t.unidad()).isEqualTo("kg");
    }

    @Test
    @DisplayName("los cortes salen de la fila que toca por sexo y edad")
    void escogeLaFila() {
        String p50 = TablasReferencia.clave("dinamometria", 50);
        String p5 = TablasReferencia.clave("dinamometria", 5);

        assertThat(tablas.corte(p50, false, 22).texto(1)).isEqualTo("48.0");   // H 20–24
        assertThat(tablas.corte(p50, false, 24).texto(1)).isEqualTo("48.0");   // el tope es inclusivo
        assertThat(tablas.corte(p50, false, 25).texto(1)).isEqualTo("49.3");   // H 25–29
        assertThat(tablas.corte(p5, true, 22).texto(1)).isEqualTo("19.7");     // M 20–24
        assertThat(tablas.corte(p5, true, 63).texto(1)).isEqualTo("15.2");     // M 60–64
        assertThat(tablas.corte(p5, false, 120).texto(1)).isEqualTo("8.8");    // H 100+
        assertThat(tablas.corte(p5, true, 100).texto(1)).isEqualTo("4.2");     // M 100+
        assertThat(tablas.corte(p5, false, 22).unidad()).isEqualTo(Unidad.de("kg"));
    }

    @Test
    @DisplayName("sin sexo, sin edad o fuera de la tabla no hay valor, no uno parecido")
    void loQueNoSeSabeQuedaAusente() {
        String p50 = TablasReferencia.clave("dinamometria", 50);

        assertThat(tablas.corte(p50, null, 30).ausente()).isTrue();
        assertThat(tablas.corte(p50, false, null).ausente()).isTrue();
        assertThat(tablas.corte(p50, false, 19).ausente()).as("la tabla empieza a los 20").isTrue();
        assertThat(tablas.corte("referencia.inexistente.p50", false, 30).ausente()).isTrue();
        assertThat(tablas.corte("referencia.dinamometria.p55", false, 30).ausente()).isTrue();
    }

    @Test
    @DisplayName("la validación detecta una fila que no crece")
    void detectaUnaTablaRota() {
        TablaReferencia buena = tablas.tabla("dinamometria").orElseThrow();
        var rota = new TablaReferencia.Fila("H", 20, 24, List.of(
                new BigDecimal("33.9"), new BigDecimal("30.0"), new BigDecimal("40.5"),
                new BigDecimal("43.2"), new BigDecimal("45.7"), new BigDecimal("48.0"),
                new BigDecimal("50.4"), new BigDecimal("52.9"), new BigDecimal("56.0"),
                new BigDecimal("60.1"), new BigDecimal("63.6")));
        var filas = new java.util.ArrayList<>(buena.filas());
        filas.set(0, rota);

        var t = new TablaReferencia(buena.id(), buena.nombre(), buena.version(), buena.unidad(),
                buena.fuente(), buena.edadMinima(), buena.cortes(), filas);

        assertThat(t.problemas()).anyMatch(p -> p.contains("no crecen"));
    }

    // ── La interpolación ─────────────────────────────────────────────────────

    @Test
    @DisplayName("entre dos cortes se interpola en línea recta")
    void interpola() {
        // H 20–24: P40 = 45.7 y P50 = 48.0; 46.85 queda a mitad de camino.
        assertThat(evaluar(PERCENTIL, hombreDe22("46.85")).texto(1)).isEqualTo("45.0");
        // Justo en un corte, el percentil es ese corte.
        assertThat(evaluar(PERCENTIL, hombreDe22("48.0")).texto(1)).isEqualTo("50.0");
    }

    @Test
    @DisplayName("fuera del P5–P95 no hay percentil numérico")
    void fueraDeLaTabla() {
        assertThat(evaluar(PERCENTIL, hombreDe22("30")).ausente()).isTrue();
        assertThat(evaluar(PERCENTIL, hombreDe22("70")).ausente()).isTrue();
    }

    @Test
    @DisplayName("la banda dice el tramo y nombra los extremos con palabras")
    void banda() {
        assertThat(evaluar(BANDA, hombreDe22("46.85")).texto(null)).isEqualTo("P40–P50");
        assertThat(evaluar(BANDA, hombreDe22("48.0")).texto(null)).isEqualTo("P50");
        assertThat(evaluar(BANDA, hombreDe22("33.9")).texto(null)).isEqualTo("P5");
        assertThat(evaluar(BANDA, hombreDe22("30")).texto(null)).isEqualTo("< P5");
        assertThat(evaluar(BANDA, hombreDe22("70")).texto(null)).isEqualTo("> P95");
    }

    @Test
    @DisplayName("si no se midió o no hay tabla para la persona, la celda queda vacía")
    void sinDato() {
        Map<String, Magnitud> sinFuerza = hombreDe22("40");
        sinFuerza.remove("fuerza");
        assertThat(evaluar(BANDA, sinFuerza).ausente()).isTrue();

        // Menor de 20: los cortes no existen y el resultado se contagia.
        Map<String, Magnitud> joven = new HashMap<>();
        joven.put("fuerza", Magnitud.de(new BigDecimal("40"), Unidad.de("kg")));
        for (int p : Percentiles.PERCENTILES) {
            joven.put("p" + p, tablas.corte(TablasReferencia.clave("dinamometria", p), false, 18));
        }
        assertThat(evaluar(BANDA, joven).ausente()).isTrue();
    }

    @Test
    @DisplayName("el rango esperado P5–P95 se arma con las mismas variables")
    void rangoEsperado() {
        Map<String, Magnitud> datos = hombreDe22("50");
        assertThat(evaluar("p5", datos).texto(1)).isEqualTo("33.9");
        assertThat(evaluar("p95", datos).texto(1)).isEqualTo("63.6");
        assertThat(evaluar("si(fuerza < p5, 1, 0)", datos).texto(0)).isEqualTo("0");
    }

    @Test
    @DisplayName("faltan cortes: se avisa al escribir la fórmula, no al emitir")
    void argumentosIncompletos() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> AnalizadorFormula.analizar("percentil(fuerza, p5, p10)"))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("cortes");
    }
}
