package imss.gob.mx.cohorte.services.importacion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El parseo de CSV es donde fallan estas importaciones en la practica, y falla
 * en silencio: no lanza error, importa datos corridos de columna.
 */
class ParserCsvTest {

    @Test
    void unaComaDentroDeComillasNoParteElCampo() {
        // Este es el caso que rompe al partir por el separador a secas: el nombre
        // se convertia en dos campos y el folio terminaba leyendose del sexo.
        List<String> campos = ParserCsv.partir("\"Gomez, Juan\",000502,M", ',');
        assertEquals(List.of("Gomez, Juan", "000502", "M"), campos);
    }

    @Test
    void comillasDoblesDentroDelCampoSonUnaComillaLiteral() {
        List<String> campos = ParserCsv.partir("\"Dijo \"\"hola\"\"\",x", ',');
        assertEquals(List.of("Dijo \"hola\"", "x"), campos);
    }

    @Test
    void camposVaciosSeConservanEnSuPosicion() {
        // Perder los vacios desplazaria todo lo que viene detras.
        assertEquals(List.of("a", "", "c"), ParserCsv.partir("a,,c", ','));
        assertEquals(List.of("", "b"), ParserCsv.partir(",b", ','));
        assertEquals(List.of("a", ""), ParserCsv.partir("a,", ','));
    }

    @Test
    void unaComillaEnMitadDeUnValorEsTexto() {
        // 5" de altura no abre un campo entrecomillado.
        assertEquals(List.of("5\" pulgadas", "x"), ParserCsv.partir("5\" pulgadas,x", ','));
    }

    @Test
    void detectaElPuntoYComaDeExcelEnEspanol() {
        String enc = "fecha;folio;peso;estatura";
        assertEquals(';', ParserCsv.detectarSeparador(enc));
        assertEquals(4, ParserCsv.partir(enc, ';').size());
    }

    @Test
    void detectaLaComaCuandoEsElSeparador() {
        assertEquals(',', ParserCsv.detectarSeparador("fecha,folio,peso,estatura"));
    }

    @Test
    void unSeparadorDentroDeComillasNoConfundeLaDeteccion() {
        // El punto y coma aparece 3 veces como separador; la coma solo dentro del
        // texto entrecomillado, asi que no debe ganar.
        assertEquals(';', ParserCsv.detectarSeparador("a;\"uno, dos\";c;d"));
    }

    @Test
    void unaSolaColumnaSigueSiendoUnaFila() {
        assertEquals(List.of("solo"), ParserCsv.partir("solo", ','));
    }
}
