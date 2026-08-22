package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.services.importacion.ConversorValor.ValorNoValidoException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El sitio donde una carga masiva mete datos falsos sin fallar: un "72,5" leido
 * como 725, un "N/A" convertido en 0, un "No" guardado como verdadero.
 */
class ConversorValorTest {

    private static OpcionParametro opcion(String valor) {
        OpcionParametro o = new OpcionParametro();
        o.setValor(valor);
        return o;
    }

    private static double numero(String crudo) {
        return ConversorValor.convertir(crudo, TipoParametro.NUMERICO, null).numerico();
    }

    private static boolean booleano(String crudo) {
        return ConversorValor.convertir(crudo, TipoParametro.BOOLEANO, null).booleano();
    }

    // ── Numeros ──────────────────────────────────────────────────────────────

    @Test
    void elPuntoYLaComaDecimalDanElMismoNumero() {
        assertEquals(72.5, numero("72.5"));
        assertEquals(72.5, numero("72,5"));
    }

    @Test
    void elSeparadorDeMilesNoMultiplicaElValor() {
        // Leer "1,234" como 1234 esta bien; leerlo como 1.234 cambiaria el dato
        // por mil. Se distingue por el patron de tres cifras.
        assertEquals(1234.0, numero("1,234"));
        assertEquals(1234.0, numero("1.234,0"));
        assertEquals(1234.5, numero("1,234.5"));
    }

    @Test
    void unaComaConDosDecimalesNoEsSeparadorDeMiles() {
        assertEquals(1.23, numero("1,23"));
    }

    @Test
    void unPuntoConTresDecimalesSeLeeComoMiles() {
        // Caso deliberadamente ambiguo: "1.234" en un archivo en espanol casi
        // siempre son mil doscientos treinta y cuatro.
        assertEquals(1234.0, numero("1.234"));
    }

    @Test
    void laUnidadPegadaAlNumeroNoEstorba() {
        assertEquals(72.5, numero("72.5 kg"));
        assertEquals(72.5, numero("72.5kg"));
        assertEquals(18.0, numero("18 %"));
        assertEquals(18.0, numero("18%"));
    }

    @Test
    void losNegativosSeConservan() {
        assertEquals(-1.5, numero("-1.5"));
        // Algunos exportadores usan el minus matematico en vez del guion.
        assertEquals(-1.5, numero("−1.5"));
    }

    @Test
    void unTextoEnUnaColumnaNumericaSeRechaza() {
        // Dos caminos distintos: "normal" es todo letras y no queda nada que
        // parsear; "12a34" si tiene cifras pero no forma un numero. Los dos
        // tienen que fallar y decir que se esperaba un numero.
        for (String texto : List.of("normal", "12a34", "??")) {
            var e = assertThrows(ValorNoValidoException.class, () -> numero(texto));
            assertTrue(e.getMessage().contains("numero"), texto + " -> " + e.getMessage());
            assertTrue(e.getMessage().contains(texto), "el mensaje debe citar el valor: " + e.getMessage());
        }
    }

    @Test
    void laNotacionCientificaNoSeConfundeConUnaUnidad() {
        // La regla que quita unidades pegadas mira las letras finales; aqui la
        // "E" va en medio y el valor termina en cifra, asi que no debe tocarse.
        assertEquals(1500.0, numero("1.5E3"));
    }

    // ── Ausencia de dato ─────────────────────────────────────────────────────

    @Test
    void naNoSeConvierteEnCero() {
        // Convertirlo en 0 dejaria el estudio con un dato inventado que nadie
        // volveria a revisar.
        for (String ausente : List.of("N/A", "n/a", "ND", "-", "sin dato", "#N/A")) {
            assertThrows(ValorNoValidoException.class,
                    () -> numero(ausente), "deberia rechazar " + ausente);
        }
    }

    @Test
    void laCeldaVaciaSeRechazaPorqueTodoEsObligatorio() {
        var e = assertThrows(ValorNoValidoException.class, () -> numero("   "));
        assertTrue(e.getMessage().contains("obligatorio"), e.getMessage());
    }

    @Test
    void elCeroSiEsUnValorValido() {
        // No confundir "vacio" con "cero": 0 es un resultado legitimo.
        assertEquals(0.0, numero("0"));
    }

    // ── Booleanos ────────────────────────────────────────────────────────────

    @Test
    void reconoceLasFormasHabitualesDeSiYNo() {
        for (String si : List.of("Si", "SÍ", "sí", "S", "1", "X", "true", "VERDADERO", "Positivo")) {
            assertTrue(booleano(si), si + " deberia ser verdadero");
        }
        for (String no : List.of("No", "NO", "N", "0", "false", "FALSO", "Negativo")) {
            assertFalse(booleano(no), no + " deberia ser falso");
        }
    }

    @Test
    void unBooleanoQueNoSeEntiendeSeRechazaEnVezDeSuponerFalso() {
        // Suponer falso es lo peligroso: se guardaria una respuesta que nadie dio.
        var e = assertThrows(ValorNoValidoException.class, () -> booleano("quiza"));
        assertTrue(e.getMessage().contains("si o no"), e.getMessage());
    }

    // ── Opciones ─────────────────────────────────────────────────────────────

    @Test
    void laOpcionSeEmparejaSinAcentosNiMayusculas() {
        var r = ConversorValor.convertir("izquierda", TipoParametro.TEXTO_OPCIONES,
                List.of(opcion("Izquierda"), opcion("Derecha")));
        // Se guarda la del catalogo, no la del archivo, o el mismo parametro
        // acabaria con dos escrituras del mismo valor.
        assertEquals("Izquierda", r.texto());
    }

    @Test
    void unaOpcionFueraDelCatalogoSeRechazaDiciendoCualesValen() {
        var e = assertThrows(ValorNoValidoException.class,
                () -> ConversorValor.convertir("Ambas", TipoParametro.TEXTO_OPCIONES,
                        List.of(opcion("Izquierda"), opcion("Derecha"))));
        assertTrue(e.getMessage().contains("Izquierda, Derecha"), e.getMessage());
    }

    @Test
    void unParametroDeSeleccionSinOpcionesConfiguradasSeSenalaComoTal() {
        var e = assertThrows(ValorNoValidoException.class,
                () -> ConversorValor.convertir("lo que sea", TipoParametro.TEXTO_OPCIONES, List.of()));
        assertTrue(e.getMessage().contains("catalogo"), e.getMessage());
    }

    // ── Texto ────────────────────────────────────────────────────────────────

    @Test
    void elTextoSeGuardaTalCual() {
        var r = ConversorValor.convertir("  Sin hallazgos  ", TipoParametro.TEXTO, null);
        assertEquals("Sin hallazgos", r.texto());
    }

    @Test
    void unTextoMasLargoQueLaColumnaSeRechazaAntesDeGuardarlo() {
        // Si no, la base lo trunca en silencio y se pierde la mitad del hallazgo.
        var e = assertThrows(ValorNoValidoException.class,
                () -> ConversorValor.convertir("x".repeat(256), TipoParametro.TEXTO, null));
        assertTrue(e.getMessage().contains("255"), e.getMessage());
    }

    // ── Cada tipo llena solo su campo ────────────────────────────────────────

    @Test
    void soloVieneRellenoElCampoDelTipoQueCorresponde() {
        var n = ConversorValor.convertir("5", TipoParametro.NUMERICO, null);
        assertNotNull(n.numerico());
        assertNull(n.texto());
        assertNull(n.booleano());

        var b = ConversorValor.convertir("Si", TipoParametro.BOOLEANO, null);
        assertNotNull(b.booleano());
        assertNull(b.numerico());
    }
}
