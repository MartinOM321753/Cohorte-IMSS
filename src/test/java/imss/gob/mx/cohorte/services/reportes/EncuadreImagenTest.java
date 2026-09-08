package imss.gob.mx.cohorte.services.reportes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Cómo se coloca una imagen dentro de su caja.
 *
 * <p>Esto existe por un fallo que no daba ninguna señal: el motor de PDF no conoce
 * {@code object-fit}, así que «entera» y «recortada» se veían correctas en el
 * editor y salían estiradas en el papel. Nadie lo notaba hasta comparar contra la
 * impresión.</p>
 *
 * <p>Las cuentas de aquí tienen que coincidir con las de {@code encuadreDeImagen}
 * en el editor. Si divergen, la vista previa vuelve a mentir.</p>
 */
class EncuadreImagenTest {

    private static final double TOL = 0.001;

    /** Una imagen apaisada 2:1 en una caja cuadrada de 100×100. */
    private EncuadreImagen enCajaCuadrada(String ajuste) {
        return EncuadreImagen.de(100, 100, 400, 200, ajuste, 1, 0, 0);
    }

    @Test
    @DisplayName("«entera» cabe completa y se centra")
    void contenerCabeEntera() {
        EncuadreImagen e = enCajaCuadrada("contener");

        // 2:1 en una caja cuadrada: manda el ancho, y sobra aire arriba y abajo.
        assertThat(e.anchoMm()).isCloseTo(100, within(TOL));
        assertThat(e.altoMm()).isCloseTo(50, within(TOL));
        assertThat(e.izquierdaMm()).isCloseTo(0, within(TOL));
        assertThat(e.arribaMm()).isCloseTo(25, within(TOL));
    }

    @Test
    @DisplayName("«recortada» llena la caja y se sale por los lados")
    void cubrirLlenaLaCaja() {
        EncuadreImagen e = enCajaCuadrada("cubrir");

        // Para que el alto llegue a 100 el ancho tiene que ser 200: sobra a los lados,
        // y lo que sobra queda recortado a partes iguales.
        assertThat(e.anchoMm()).isCloseTo(200, within(TOL));
        assertThat(e.altoMm()).isCloseTo(100, within(TOL));
        assertThat(e.izquierdaMm()).isCloseTo(-50, within(TOL));
        assertThat(e.arribaMm()).isCloseTo(0, within(TOL));
    }

    @Test
    @DisplayName("«estirar» ocupa la caja exacta, deformando")
    void estirarDeforma() {
        EncuadreImagen e = enCajaCuadrada("estirar");

        assertThat(e.anchoMm()).isCloseTo(100, within(TOL));
        assertThat(e.altoMm()).isCloseTo(100, within(TOL));
        assertThat(e.izquierdaMm()).isCloseTo(0, within(TOL));
        assertThat(e.arribaMm()).isCloseTo(0, within(TOL));
    }

    @Test
    @DisplayName("sin las medidas de la imagen se estira, en vez de fallar")
    void sinMedidasSeEstira() {
        // Pasa con una imagen subida antes de que se guardaran las medidas. Estirar
        // es lo que hacía el sistema hasta ahora: peor sería no dibujar nada.
        assertThat(EncuadreImagen.de(100, 40, null, null, "contener", 1, 0, 0))
                .isEqualTo(EncuadreImagen.estirado(100, 40));
        assertThat(EncuadreImagen.de(100, 40, 0, 0, "cubrir", 1, 0, 0))
                .isEqualTo(EncuadreImagen.estirado(100, 40));
    }

    @Test
    @DisplayName("«libre» parte de «entera» y aplica el acercamiento")
    void libreParteDeEntera() {
        EncuadreImagen sinZoom = EncuadreImagen.de(100, 100, 400, 200, "libre", 1, 0, 0);
        assertThat(sinZoom.anchoMm()).isCloseTo(100, within(TOL));

        // Que el 1 sea «entera» y no el tamaño original es lo que hace que sustituir
        // la imagen por otra de distinta resolución no descoloque el encuadre.
        EncuadreImagen doble = EncuadreImagen.de(100, 100, 400, 200, "libre", 2, 0, 0);
        assertThat(doble.anchoMm()).isCloseTo(200, within(TOL));
        assertThat(doble.altoMm()).isCloseTo(100, within(TOL));
        assertThat(doble.izquierdaMm()).isCloseTo(-50, within(TOL));
    }

    @Test
    @DisplayName("«libre» corre la imagen desde el centro")
    void libreSeDesplaza() {
        EncuadreImagen e = EncuadreImagen.de(100, 100, 400, 200, "libre", 1, 10, -5);

        assertThat(e.izquierdaMm()).isCloseTo(10, within(TOL));
        assertThat(e.arribaMm()).isCloseTo(20, within(TOL));  // 25 centrado, menos 5
    }

    @Test
    @DisplayName("el acercamiento no puede llegar a cero")
    void zoomConMinimo() {
        // Un cero dejaría la imagen sin tamaño y sin forma de volver a agarrarla.
        EncuadreImagen e = EncuadreImagen.de(100, 100, 400, 200, "libre", 0, 0, 0);

        assertThat(e.anchoMm()).isGreaterThan(0);
    }

    @Test
    @DisplayName("el zoom y el desplazamiento no afectan a los demás modos")
    void soloLibreLosUsa() {
        EncuadreImagen e = EncuadreImagen.de(100, 100, 400, 200, "contener", 3, 40, 40);

        assertThat(e.anchoMm()).isCloseTo(100, within(TOL));
        assertThat(e.izquierdaMm()).isCloseTo(0, within(TOL));
        assertThat(e.arribaMm()).isCloseTo(25, within(TOL));
    }

    @Test
    @DisplayName("una imager más alta que ancha se resuelve por el otro lado")
    void imagenVertical() {
        // 1:2 en una caja cuadrada: ahora manda el alto.
        EncuadreImagen e = EncuadreImagen.de(100, 100, 200, 400, "contener", 1, 0, 0);

        assertThat(e.anchoMm()).isCloseTo(50, within(TOL));
        assertThat(e.altoMm()).isCloseTo(100, within(TOL));
        assertThat(e.izquierdaMm()).isCloseTo(25, within(TOL));
        assertThat(e.arribaMm()).isCloseTo(0, within(TOL));
    }

    @Test
    @DisplayName("sin ajuste guardado se comporta como «entera»")
    void porDefectoEntera() {
        assertThat(EncuadreImagen.de(100, 100, 400, 200, null, 1, 0, 0))
                .isEqualTo(enCajaCuadrada("contener"));
    }
}
