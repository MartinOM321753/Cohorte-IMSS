package imss.gob.mx.cohorte.services.importacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cómo se decide qué significa cada columna de la plantilla de muestras.
 *
 * <p>El archivo lo escribe una persona, no un instrumento, así que la misma
 * columna llega titulada de varias formas. Lo que no puede pasar es que dos
 * columnas acaben apuntando al mismo campo en silencio: el dato entraría en el
 * sitio equivocado y parecería correcto.</p>
 */
class ColumnaMuestraTest {

    private static final List<String> MINIMAS =
            List.of("folio", "tipoMuestra", "tubo", "numeroAlicuota");

    @Test
    @DisplayName("Los encabezados de la plantilla se reconocen todos")
    void reconoceLaPlantilla() {
        var e = ColumnaMuestra.emparejar(List.of(
                "folio", "tipoMuestra", "tubo", "fecha", "numeroAlicuota",
                "volumen", "unidad", "codigoCaja", "posicion", "observaciones"));

        assertTrue(e.sinProblemas(), () -> "problemas: " + e.problemas());
        assertTrue(e.ignoradas().isEmpty(), () -> "ignoradas: " + e.ignoradas());
        for (ColumnaMuestra c : ColumnaMuestra.values()) {
            assertTrue(e.trae(c), "no reconoció " + c);
        }
    }

    @Test
    @DisplayName("Da igual el acento, la mayúscula y la puntuación del título")
    void toleraComoSeEscriba() {
        var e = ColumnaMuestra.emparejar(List.of(
                "FOLIO", "Tipo de Muestra", "TUBO", "No. alícuota", "UBICACIÓN"));

        assertTrue(e.sinProblemas(), () -> "problemas: " + e.problemas());
        assertEquals(1, e.indiceDe(ColumnaMuestra.TIPO_MUESTRA));
        assertEquals(3, e.indiceDe(ColumnaMuestra.NUMERO_ALICUOTA));
        assertEquals(4, e.indiceDe(ColumnaMuestra.POSICION));
    }

    @Test
    @DisplayName("«MUESTRA» a secas es el número de alícuota, como lo titulaba la hoja original")
    void muestraEsElNumeroDeAlicuota() {
        var e = ColumnaMuestra.emparejar(List.of("FOLIO", "TIPO", "TUBO", "MUESTRA", "CAJA"));

        assertTrue(e.sinProblemas(), () -> "problemas: " + e.problemas());
        assertEquals(3, e.indiceDe(ColumnaMuestra.NUMERO_ALICUOTA));
        assertEquals(1, e.indiceDe(ColumnaMuestra.TIPO_MUESTRA));
        assertEquals(4, e.indiceDe(ColumnaMuestra.CODIGO_CAJA));
    }

    @Test
    @DisplayName("Falta una columna obligatoria: se dice cuál y no se sigue")
    void exigeLasObligatorias() {
        var e = ColumnaMuestra.emparejar(List.of("folio", "tipoMuestra", "posicion"));

        assertFalse(e.sinProblemas());
        // Se citan con el título que escribe la plantilla, no con su forma
        // normalizada: el usuario tiene que poder buscarlo en su hoja.
        assertTrue(e.problemas().stream().anyMatch(p -> p.contains("\"tubo\"")), () -> "" + e.problemas());
        assertTrue(e.problemas().stream().anyMatch(p -> p.contains("\"numeroAlicuota\"")),
                () -> "" + e.problemas());
    }

    @Test
    @DisplayName("La fecha NO es obligatoria: sin columna se elige una para todo el archivo")
    void laFechaPuedeFaltar() {
        var e = ColumnaMuestra.emparejar(MINIMAS);

        assertTrue(e.sinProblemas(), () -> "problemas: " + e.problemas());
        assertFalse(e.trae(ColumnaMuestra.FECHA));
        assertEquals(-1, e.indiceDe(ColumnaMuestra.FECHA));
    }

    /**
     * El caso que de verdad importa: dos columnas al mismo campo. Elegir una en
     * silencio guardaría un dato y descartaría el otro sin que nadie lo sepa.
     */
    @Test
    @DisplayName("Dos columnas que dicen lo mismo detienen la carga, y se nombran las dos")
    void rechazaColumnasRepetidas() {
        var e = ColumnaMuestra.emparejar(
                List.of("folio", "tipoMuestra", "tubo", "numeroAlicuota", "volumen", "CANTIDAD"));

        assertFalse(e.sinProblemas());
        String problema = e.problemas().get(0);
        assertTrue(problema.contains("volumen"), problema);
        assertTrue(problema.contains("CANTIDAD"), problema);
    }

    /**
     * La plantilla lleva el nombre del participante y la caja de origen para que
     * una persona pueda leer la hoja. Avisar de ellas como «columna desconocida»
     * sería avisar de nuestras propias columnas, y el usuario aprendería a
     * ignorar los avisos.
     */
    @Test
    @DisplayName("Las columnas informativas de la plantilla se ignoran sin avisar")
    void lasInformativasNoSeAvisan() {
        var e = ColumnaMuestra.emparejar(List.of(
                "folio", "tipoMuestra", "tubo", "numeroAlicuota", "nombre", "cajaOrigen", "CONSECUTIVO"));

        assertTrue(e.sinProblemas(), () -> "problemas: " + e.problemas());
        assertTrue(e.ignoradas().isEmpty(), () -> "no debería avisar de: " + e.ignoradas());
    }

    @Test
    @DisplayName("Una columna desconocida sí se avisa, pero no detiene la carga")
    void lasDesconocidasSeAvisan() {
        var e = ColumnaMuestra.emparejar(List.of(
                "folio", "tipoMuestra", "tubo", "numeroAlicuota", "temperatura del congelador"));

        assertTrue(e.sinProblemas(), () -> "problemas: " + e.problemas());
        assertEquals(List.of("temperatura del congelador"), e.ignoradas());
    }

    @Test
    @DisplayName("Una columna sin título no es un error ni un aviso")
    void toleraEncabezadosVacios() {
        var e = ColumnaMuestra.emparejar(List.of("folio", "tipoMuestra", "tubo", "numeroAlicuota", "", "   "));

        assertTrue(e.sinProblemas(), () -> "problemas: " + e.problemas());
        assertTrue(e.ignoradas().isEmpty(), () -> "ignoradas: " + e.ignoradas());
    }
}
