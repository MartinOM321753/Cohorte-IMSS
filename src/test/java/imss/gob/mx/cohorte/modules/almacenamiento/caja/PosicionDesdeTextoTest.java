package imss.gob.mx.cohorte.modules.almacenamiento.caja;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Leer un hueco escrito a mano y volver a escribirlo igual.
 *
 * <p>La ida ya existía —la rejilla se pinta con letras— y la vuelta la necesita
 * la carga masiva, que recibe {@code A1} y tiene que guardar dos enteros. Las
 * dos direcciones viven juntas justamente para que no puedan separarse: si una
 * cambiara sola, un vial cargado quedaría en un hueco distinto del que dice su
 * etiqueta.</p>
 */
class PosicionDesdeTextoTest {

    @Test
    @DisplayName("Lo que escribe la rejilla se vuelve a leer igual")
    void idaYVuelta() {
        for (int fila = 1; fila <= 60; fila++) {
            for (int columna = 1; columna <= 12; columna++) {
                String etiqueta = EtiquetaPosicionCaja.etiqueta(fila, columna);
                var coord = EtiquetaPosicionCaja.parsear(etiqueta);
                assertNotNull(coord, etiqueta);
                assertEquals(fila, coord.fila(), etiqueta);
                assertEquals(columna, coord.columna(), etiqueta);
            }
        }
    }

    /**
     * Más de 26 filas es donde se rompe el atajo {@code (char)('A' + n)}, que es
     * el motivo de que la conversión sea en base 26 y no una resta.
     */
    @Test
    @DisplayName("Pasadas las 26 filas la letra es doble")
    void masDeVeintiseisFilas() {
        assertEquals(26, EtiquetaPosicionCaja.parsear("Z1").fila());
        assertEquals(27, EtiquetaPosicionCaja.parsear("AA1").fila());
        assertEquals(52, EtiquetaPosicionCaja.parsear("AZ9").fila());
        assertEquals(53, EtiquetaPosicionCaja.parsear("BA9").fila());
    }

    @Test
    @DisplayName("Se admite como lo teclee la gente: minúsculas y espacios")
    void toleraComoSeTeclee() {
        assertEquals(new EtiquetaPosicionCaja.Coordenada(2, 7), EtiquetaPosicionCaja.parsear("b7"));
        assertEquals(new EtiquetaPosicionCaja.Coordenada(2, 7), EtiquetaPosicionCaja.parsear(" B 7 "));
        assertEquals(new EtiquetaPosicionCaja.Coordenada(1, 10), EtiquetaPosicionCaja.parsear("a10"));
    }

    /**
     * Devolver null y no lanzar es deliberado: quien llama redacta el error con
     * el número de fila del archivo, que es lo que el usuario necesita para ir a
     * buscarlo en su hoja de cálculo.
     */
    @Test
    @DisplayName("Lo que no es un hueco devuelve null en vez de inventarse uno")
    void rechazaLoQueNoEsUnHueco() {
        assertNull(EtiquetaPosicionCaja.parsear(null));
        assertNull(EtiquetaPosicionCaja.parsear(""));
        assertNull(EtiquetaPosicionCaja.parsear("   "));
        assertNull(EtiquetaPosicionCaja.parsear("A"), "sin columna");
        assertNull(EtiquetaPosicionCaja.parsear("7"), "sin fila");
        assertNull(EtiquetaPosicionCaja.parsear("1A"), "al revés");
        assertNull(EtiquetaPosicionCaja.parsear("A0"), "la columna empieza en 1");
        assertNull(EtiquetaPosicionCaja.parsear("A1B"), "letra al final");
        assertNull(EtiquetaPosicionCaja.parsear("Ñ1"), "fuera del alfabeto de la rejilla");
        assertNull(EtiquetaPosicionCaja.parsear("A99999999999"), "no cabe en un int");
    }
}
