package imss.gob.mx.cohorte.services.formulas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El tipo con el que calcula el motor: número, unidad y ausencia.
 *
 * <p>Las tres cosas que se prueban aquí son las tres que, mal resueltas, imprimen un
 * número equivocado sin que nada falle: la conversión entre unidades, el redondeo, y
 * qué pasa cuando el dato no está.</p>
 */
class MagnitudTest {

    private static final Unidad CM     = Unidad.de("cm");
    private static final Unidad METRO  = Unidad.de("m");
    private static final Unidad KG     = Unidad.de("kg");

    @Test
    @DisplayName("la estatura pasa de centímetros a metros sin perder exactitud")
    void deCentimetrosAMetros() {
        Magnitud enCm = Magnitud.de(new BigDecimal("165"), CM);
        assertThat(enCm.en(METRO).texto(2)).isEqualTo("1.65");
    }

    @Test
    @DisplayName("el caso que motivó todo esto: el índice de masa corporal")
    void elIndiceDeMasaCorporal() {
        // Un participante de 68 kg y 1.65 m. Con la estatura en centímetros —que es
        // como está guardada— la división da 0.0025 en vez de 24.98.
        Magnitud estaturaGuardada = Magnitud.de(new BigDecimal("165"), CM);
        Magnitud peso = Magnitud.de(new BigDecimal("68"), KG);

        BigDecimal enMetros = estaturaGuardada.en(METRO).valor();
        BigDecimal imc = peso.valor().divide(enMetros.multiply(enMetros), Magnitud.PRECISION);

        assertThat(imc.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                .isEqualTo("24.98");
    }

    @Test
    @DisplayName("convertir a una unidad de otra dimensión deja ausente, no el número crudo")
    void loQueNoSePuedeConvertirNoSeInventa() {
        Magnitud estatura = Magnitud.de(new BigDecimal("165"), CM);

        // Devolver 165 con la etiqueta «kg» sería imprimir un dato falso con aspecto
        // de bueno. Mejor la celda en blanco.
        assertThat(estatura.en(KG).ausente()).isTrue();
        assertThat(estatura.en(Unidad.de("Según equipo")).ausente()).isTrue();
    }

    @Test
    @DisplayName("ausente no es cero y se contagia")
    void ausenteNoEsCero() {
        Magnitud sinDato = Magnitud.sinDato();

        assertThat(sinDato.ausente()).isTrue();
        assertThat(sinDato.valor()).isNull();
        assertThat(sinDato.texto(2)).isEmpty();
        assertThat(sinDato.textoConUnidad(2)).isEmpty();
        assertThat(sinDato.en(METRO).ausente()).isTrue();
    }

    @Test
    @DisplayName("un resultado sin capturar llega como ausente")
    void resultadoSinCapturar() {
        assertThat(Magnitud.de((Double) null, "kg").ausente()).isTrue();
        assertThat(Magnitud.de(Double.NaN, "kg").ausente()).isTrue();
        assertThat(Magnitud.de(Double.POSITIVE_INFINITY, "kg").ausente()).isTrue();
    }

    @Test
    @DisplayName("el número decimal capturado no arrastra la basura del punto flotante")
    void sinRuidoBinario() {
        // Con el constructor directo de BigDecimal, un 0.1 se vuelve
        // 0.1000000000000000055511151231257827: fiel a la máquina, no a quien capturó.
        assertThat(Magnitud.de(0.1d, "kg").valor().toPlainString()).isEqualTo("0.1");
        assertThat(Magnitud.de(24.95d, "kg/m²").valor().toPlainString()).isEqualTo("24.95");
    }

    @Test
    @DisplayName("el redondeo es de mitad hacia arriba, y solo al imprimir")
    void redondeoAlImprimir() {
        Magnitud imc = Magnitud.de(new BigDecimal("24.95"), Unidad.de("kg/m²"));

        assertThat(imc.texto(1)).isEqualTo("25.0");
        assertThat(imc.texto(2)).isEqualTo("24.95");

        // El valor guardado no cambia: la comparación contra el rango usa este, no el
        // texto. Redondear antes de comparar es lo que convierte un 24.95 en sobrepeso.
        assertThat(imc.valor().toPlainString()).isEqualTo("24.95");
    }

    @Test
    @DisplayName("sin decimales indicados no se rellena con ceros")
    void sinDecimalesIndicados() {
        assertThat(Magnitud.de(new BigDecimal("1.6500"), METRO).texto(null)).isEqualTo("1.65");
        assertThat(Magnitud.de(new BigDecimal("70.0"), KG).texto(null)).isEqualTo("70");
    }

    @Test
    @DisplayName("la unidad se imprime junto al número")
    void conSuUnidad() {
        assertThat(Magnitud.de(new BigDecimal("24.977"), Unidad.de("kg/m²")).textoConUnidad(2))
                .isEqualTo("24.98 kg/m²");

        // Sin unidad declarada no se deja un espacio suelto al final.
        assertThat(Magnitud.de(new BigDecimal("8"), Unidad.NINGUNA).textoConUnidad(0))
                .isEqualTo("8");
    }
}
