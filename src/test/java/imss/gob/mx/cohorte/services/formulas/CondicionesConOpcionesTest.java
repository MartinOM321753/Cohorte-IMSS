package imss.gob.mx.cohorte.services.formulas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Condiciones sobre parámetros de opciones.
 *
 * <p>El motor nació solo numérico, y eso dejaba fuera reglas que en esta cohorte son
 * cotidianas: la dinamometría se reporta de la mano dominante, y cuál es viene de un
 * parámetro de opciones. Ahora un texto se puede comparar —solo comparar— y con eso la
 * regla se escribe entera.</p>
 */
class CondicionesConOpcionesTest {

    private static Magnitud calcular(String formula, Map<String, Magnitud> variables) {
        return Evaluador.evaluar(AnalizadorFormula.analizar(formula), variables::get);
    }

    private static final Map<String, Magnitud> DIESTRO = Map.of(
            "manoDominante", Magnitud.deTexto("Derecha"),
            "fuerzaDerecha", Magnitud.de(new BigDecimal("42"), Unidad.de("kg")),
            "fuerzaIzquierda", Magnitud.de(new BigDecimal("38"), Unidad.de("kg")));

    private static final Map<String, Magnitud> ZURDO = Map.of(
            "manoDominante", Magnitud.deTexto("Izquierda"),
            "fuerzaDerecha", Magnitud.de(new BigDecimal("42"), Unidad.de("kg")),
            "fuerzaIzquierda", Magnitud.de(new BigDecimal("38"), Unidad.de("kg")));

    private static final String FUERZA_DOMINANTE =
            "si(manoDominante = 'Derecha', fuerzaDerecha, fuerzaIzquierda)";

    @Test
    @DisplayName("la fuerza de la mano dominante sale de una condición sobre el texto")
    void laManoDominante() {
        assertThat(calcular(FUERZA_DOMINANTE, DIESTRO).texto(null)).isEqualTo("42");
        assertThat(calcular(FUERZA_DOMINANTE, ZURDO).texto(null)).isEqualTo("38");
    }

    @Test
    @DisplayName("las comillas valen simples o dobles")
    void lasDosComillas() {
        assertThat(calcular("si(manoDominante = \"Derecha\", 1, 0)", DIESTRO).texto(null))
                .isEqualTo("1");
    }

    @Test
    @DisplayName("comparar no distingue mayúsculas ni espacios sobrantes")
    void comparacionIndulgente() {
        // Las opciones se capturan a mano en el catálogo: que una regla clínica dependa
        // de si alguien escribió «derecha» con minúscula sería una trampa.
        assertThat(calcular("si(manoDominante = 'DERECHA', 1, 0)", DIESTRO).texto(null))
                .isEqualTo("1");
        assertThat(calcular("si(manoDominante = ' derecha ', 1, 0)", DIESTRO).texto(null))
                .isEqualTo("1");
    }

    @Test
    @DisplayName("distinto de también funciona")
    void distintoDe() {
        assertThat(calcular("si(manoDominante <> 'Derecha', 1, 0)", ZURDO).texto(null))
                .isEqualTo("1");
    }

    @Test
    @DisplayName("preguntar si un texto es menor que otro no significa nada")
    void sinOrdenAlfabetico() {
        // Contestar por orden alfabético sería inventar un criterio que nadie pidió.
        assertThat(calcular("manoDominante < 'Izquierda'", DIESTRO).ausente()).isTrue();
    }

    @Test
    @DisplayName("un texto no se suma ni se divide")
    void nadaDeAritmeticaConTexto() {
        // «Derecha» no vale cero: vale nada. La celda queda en blanco.
        assertThat(calcular("manoDominante + 1", DIESTRO).ausente()).isTrue();
        assertThat(calcular("fuerzaDerecha / manoDominante", DIESTRO).ausente()).isTrue();
    }

    @Test
    @DisplayName("sin la opción registrada la regla no elige rama por descarte")
    void sinDatoNoSeElige() {
        Map<String, Magnitud> sinMano = Map.of(
                "fuerzaDerecha", Magnitud.de(new BigDecimal("42"), Unidad.de("kg")),
                "fuerzaIzquierda", Magnitud.de(new BigDecimal("38"), Unidad.de("kg")));

        assertThat(calcular(FUERZA_DOMINANTE, sinMano).ausente()).isTrue();
    }

    @Test
    @DisplayName("unas comillas sin cerrar se avisan al escribir")
    void comillasSinCerrar() {
        assertThatThrownBy(() -> AnalizadorFormula.analizar("si(mano = 'Derecha, 1, 0)"))
                .isInstanceOf(ErrorDeFormula.class)
                .hasMessageContaining("comillas");
    }

    @Test
    @DisplayName("un sí/no también se compara como texto")
    void booleanosComoTexto() {
        Map<String, Magnitud> conFatiga = Map.of("fatiga", Magnitud.deTexto("Sí"));
        assertThat(calcular("si(fatiga = 'Sí', 1, 0)", conFatiga).texto(null)).isEqualTo("1");
    }
}
