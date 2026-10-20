package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.variables.OperadorDependencia;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La etiqueta del papel y la dependencia entre preguntas: las dos piezas donde un error
 * no produce un fallo visible, sino un dato correcto en el expediente equivocado.
 */
class EtiquetaCuestionarioTest {

    @Nested
    @DisplayName("Leer la etiqueta del papel")
    class Etiqueta {

        @Test
        @DisplayName("la forma del volcado recibido se lee entera")
        void formaBuena() {
            var l = EtiquetaCuestionario.leer("C1/008601/F4");
            assertTrue(l.valida());
            assertEquals("C1", l.clave());
            assertEquals("008601", l.folio(), "el folio va tal como esta escrito, con sus ceros");
        }

        @Test
        @DisplayName("no se quitan los ceros de la izquierda")
        void losCerosImportan() {
            // Un folio «8601» puede existir y ser de otra persona. Normalizarlo
            // archivaria dos mil respuestas en el expediente equivocado sin una palabra.
            assertEquals("008601", EtiquetaCuestionario.leer("C1/008601/F4").folio());
            assertEquals("8601", EtiquetaCuestionario.leer("C1/8601/F4").folio());
        }

        @Test
        @DisplayName("el prefijo del consentimiento tambien se reconoce")
        void prefijoDeDosLetras() {
            var l = EtiquetaCuestionario.leer("CI/004318/F4");
            assertTrue(l.valida());
            assertEquals("CI", l.clave());
        }

        @Test
        @DisplayName("un literal que no es F4 se rechaza, no se corrige")
        void literalDistinto() {
            var l = EtiquetaCuestionario.leer("C1/008601/F5");
            assertFalse(l.valida());
            assertNotNull(l.motivo());
        }

        @Test
        @DisplayName("lo que no tiene la forma convenida se rechaza con el motivo")
        void formasMalas() {
            assertFalse(EtiquetaCuestionario.leer(null).valida());
            assertFalse(EtiquetaCuestionario.leer("").valida());
            assertFalse(EtiquetaCuestionario.leer("008601").valida());
            assertFalse(EtiquetaCuestionario.leer("C1/008601").valida());
            assertFalse(EtiquetaCuestionario.leer("C1//F4").valida());
            assertFalse(EtiquetaCuestionario.leer("C1/ABC/F4").valida());
        }

        @Test
        @DisplayName("los espacios alrededor no estorban")
        void conEspacios() {
            assertTrue(EtiquetaCuestionario.leer("  C1 / 008601 / F4 ").valida());
        }

        @Test
        @DisplayName("el folio pelado es otro formato y se reconoce aparte")
        void folioPelado() {
            // Asi llego el primer volcado: la columna se llamaba Id-Participante y traia
            // un 1. Esa forma no dice de que instrumento es la hoja.
            assertTrue(EtiquetaCuestionario.esFolioPelado("1"));
            assertTrue(EtiquetaCuestionario.esFolioPelado(" 008601 "));
            assertFalse(EtiquetaCuestionario.esFolioPelado("C1/008601/F4"));
            assertFalse(EtiquetaCuestionario.esFolioPelado(""));
        }
    }

    @Nested
    @DisplayName("De quien es la fila del volcado")
    class Folio {

        // En el volcado el instrumento lo elige quien sube el archivo, asi que de la
        // columna solo hace falta el folio. Las dos formas han llegado de verdad y las
        // dos tienen que servir; lo que no puede pasar es que una forma rara se
        // interprete a medias, porque el folio equivocado archiva dos mil respuestas
        // en el expediente de otra persona.

        @Test
        @DisplayName("la etiqueta completa entrega su folio")
        void etiquetaCompleta() {
            var f = EtiquetaCuestionario.folioDe("C1/008601/F4");
            assertTrue(f.valido());
            assertEquals("008601", f.folio());
            assertEquals("C1", f.clave());
        }

        @Test
        @DisplayName("el folio pelado tambien, y sin clave")
        void folioPelado() {
            var f = EtiquetaCuestionario.folioDe(" 000001 ");
            assertTrue(f.valido());
            assertEquals("000001", f.folio());
            assertNull(f.clave(), "el folio pelado no dice de que instrumento es la hoja");
        }

        @Test
        @DisplayName("los ceros de la izquierda se conservan en las dos formas")
        void cerosIntactos() {
            // Es lo que hace que esto sea correcto: 8601 y 008601 pueden ser dos
            // participantes distintos y el padron guarda el folio como esta escrito.
            assertEquals("008601", EtiquetaCuestionario.folioDe("C1/008601/F4").folio());
            assertEquals("008601", EtiquetaCuestionario.folioDe("008601").folio());
        }

        @Test
        @DisplayName("la clave de la etiqueta no tiene que coincidir con nada")
        void claveAjena() {
            // Antes una etiqueta C2 en un volcado cargado como C1 se rechazaba. Ahora
            // se lee igual: el instrumento lo decide la plantilla seleccionada y la
            // clave solo sirve para avisar.
            var f = EtiquetaCuestionario.folioDe("C2/000014/F4");
            assertTrue(f.valido());
            assertEquals("000014", f.folio());
            assertEquals("C2", f.clave());
        }

        @Test
        @DisplayName("lo que no es ninguna de las dos formas se rechaza con su motivo")
        void formaDesconocida() {
            for (String malo : new String[]{"C1/008601/F9", "C1-008601", "folio 8601", "C1//F4"}) {
                var f = EtiquetaCuestionario.folioDe(malo);
                assertFalse(f.valido(), "\"" + malo + "\" no deberia pasar");
                assertNotNull(f.motivo(), "un rechazo sin motivo deja al usuario sin saber que hacer");
            }
        }

        @Test
        @DisplayName("la celda vacia se rechaza diciendo que falta el folio")
        void vacia() {
            var f = EtiquetaCuestionario.folioDe("   ");
            assertFalse(f.valido());
            assertTrue(f.motivo().toLowerCase().contains("folio"));
        }
    }

    @Nested
    @DisplayName("Si la pregunta se le hizo al participante")
    class Dependencias {

        private VariableCuestionario dependiente(String llave, String valor) {
            VariableCuestionario v = new VariableCuestionario();
            v.setCodigo("P20_1");
            v.setDependeDe(llave);
            v.setValorHabilitante(valor);
            return v;
        }

        @Test
        @DisplayName("sin llave, siempre se pregunta")
        void sinLlave() {
            assertTrue(EvaluadorDependencias.habilitada(new VariableCuestionario(), Map.of()));
        }

        @Test
        @DisplayName("con la llave contestada como se espera, se pregunta")
        void llaveCoincide() {
            assertTrue(EvaluadorDependencias.habilitada(
                    dependiente("P19", "1"), Map.of("P19", "1")));
        }

        @Test
        @DisplayName("con la llave contestada de otra forma, no se pregunta")
        void llaveNoCoincide() {
            assertFalse(EvaluadorDependencias.habilitada(
                    dependiente("P19", "1"), Map.of("P19", "2")));
        }

        @Test
        @DisplayName("si la llave no esta contestada, se pregunta igual")
        void llaveSinContestar() {
            // Es el lado seguro. Al importar, tratarla como deshabilitada descartaria
            // respuestas que el archivo si trae, y una respuesta descartada no vuelve.
            assertTrue(EvaluadorDependencias.habilitada(
                    dependiente("P19", "1"), Map.of("P19", "")));
            assertTrue(EvaluadorDependencias.habilitada(
                    dependiente("P19", "1"), Map.of()));
        }

        @Test
        @DisplayName("una llave que apunta a nada es un defecto del catalogo, y se nota")
        void llaveInexistente() {
            assertTrue(EvaluadorDependencias.habilitada(
                    dependiente("P999", "1"), Map.of("P19", "1")),
                    "la pregunta tiene que aparecer, no desaparecer en silencio");
        }

        @Test
        @DisplayName("depender de algo sin decir de que valor no deshabilita")
        void sinValorHabilitante() {
            assertTrue(EvaluadorDependencias.habilitada(
                    dependiente("P19", null), Map.of("P19", "2")));
        }

        // ── La casilla de descarte ───────────────────────────────────
        //
        // El «Ninguna» que encabeza la 112: si esta marcado, las otras 135 preguntas de
        // la rejilla no se le hicieron. No se puede decir con una igualdad, porque
        // habria que enumerar todo lo que no es «Ninguna».

        private VariableCuestionario descarte(String llave, String valor) {
            VariableCuestionario v = dependiente(llave, valor);
            v.setOperadorDependencia(OperadorDependencia.DISTINTO);
            return v;
        }

        @Test
        @DisplayName("con la llave marcada, la dependiente NO se pregunta")
        void descarteMarcado() {
            assertFalse(EvaluadorDependencias.habilitada(
                    descarte("P112_1", "1"), Map.of("P112_1", "1")));
        }

        @Test
        @DisplayName("con la llave contestada de otra forma, si se pregunta")
        void descarteConOtroValor() {
            assertTrue(EvaluadorDependencias.habilitada(
                    descarte("P112_1", "1"), Map.of("P112_1", "2")));
        }

        @Test
        @DisplayName("la llave sin contestar sigue habilitando, tambien al reves")
        void descarteSinContestar() {
            // La regla de «habilita cuando no sabe» manda sobre el operador. La negacion
            // es sobre el valor contestado, no sobre la ausencia de respuesta: si no,
            // una llave vacia escondería preguntas que el archivo si trae.
            assertTrue(EvaluadorDependencias.habilitada(
                    descarte("P112_1", "1"), Map.of("P112_1", "")));
            assertTrue(EvaluadorDependencias.habilitada(
                    descarte("P112_1", "1"), Map.of()));
        }

        @Test
        @DisplayName("sin decir el operador se compara por igualdad, como siempre")
        void operadorPorOmision() {
            assertEquals(OperadorDependencia.IGUAL,
                    new VariableCuestionario().getOperadorDependencia());
            assertTrue(EvaluadorDependencias.habilitada(
                    dependiente("P19", "1"), Map.of("P19", "1")));
        }

        // ── Dos saltos desde la misma pregunta ───────────────────────────
        //
        // La 30 tiene tres respuestas y dos flechas: «nunca he fumado» salta hasta la
        // 50 y «ya no fumo» salta a la 42, asi que el tramo de la 42 a la 49 es para
        // quien contesto lo segundo O lo tercero.

        private VariableCuestionario entre(String llave, String valores) {
            VariableCuestionario v = dependiente(llave, valores);
            v.setOperadorDependencia(OperadorDependencia.EN);
            return v;
        }

        @Test
        @DisplayName("EN acepta cualquiera de los valores de la lista")
        void enAcepta() {
            assertTrue(EvaluadorDependencias.habilitada(entre("P30", "2|3"), Map.of("P30", "2")));
            assertTrue(EvaluadorDependencias.habilitada(entre("P30", "2|3"), Map.of("P30", "3")));
        }

        @Test
        @DisplayName("EN rechaza lo que no esta en la lista")
        void enRechaza() {
            assertFalse(EvaluadorDependencias.habilitada(entre("P30", "2|3"), Map.of("P30", "1")));
        }

        @Test
        @DisplayName("EN compara el valor entero, no por parecido")
        void enNoCompaaraPorSubcadena() {
            // Con un «contains» ingenuo, el 3 de «13» daria por bueno el salto.
            assertFalse(EvaluadorDependencias.habilitada(entre("P30", "2|3"), Map.of("P30", "13")));
            assertFalse(EvaluadorDependencias.habilitada(entre("P30", "12"), Map.of("P30", "1")));
        }

        @Test
        @DisplayName("con la llave sin contestar, EN tambien pregunta")
        void enSinContestar() {
            assertTrue(EvaluadorDependencias.habilitada(entre("P30", "2|3"), Map.of()));
        }

        // ── Cadenas ───────────────────────────────────────────────
        //
        // La 34 depende de la 33, que depende de la 30. Quien nunca fumo no contesta
        // la 33, asi que mirando solo la llave inmediata la 34 saldria habilitada
        // —«la llave esta vacia, no sabemos»— y se le preguntaria a alguien que ya
        // dijo que no fuma.

        @Test
        @DisplayName("si a la llave no se le pregunto, a la dependiente tampoco")
        void cadenaCortada() {
            VariableCuestionario p33 = dependiente("P30", "3");
            p33.setCodigo("P33");
            VariableCuestionario p34 = dependiente("P33", "2");
            p34.setCodigo("P34");
            Map<String, VariableCuestionario> catalogo = Map.of("P33", p33);

            // Contesto «nunca he fumado»: la 33 no se le pregunta, luego la 34 tampoco.
            assertFalse(EvaluadorDependencias.habilitada(
                    p34, Map.of("P30", "1"), catalogo),
                    "la 34 no deberia preguntarse si la 33 quedo fuera");

            // Y sin el catalogo a mano se cae a mirar solo la llave inmediata.
            assertTrue(EvaluadorDependencias.habilitada(p34, Map.of("P30", "1")));
        }

        @Test
        @DisplayName("si la llave si se pregunto, manda su propia respuesta")
        void cadenaCompleta() {
            VariableCuestionario p33 = dependiente("P30", "3");
            p33.setCodigo("P33");
            VariableCuestionario p34 = dependiente("P33", "2");
            p34.setCodigo("P34");
            Map<String, VariableCuestionario> catalogo = Map.of("P33", p33);

            assertTrue(EvaluadorDependencias.habilitada(
                    p34, Map.of("P30", "3", "P33", "2"), catalogo));
            assertFalse(EvaluadorDependencias.habilitada(
                    p34, Map.of("P30", "3", "P33", "1"), catalogo));
        }

        @Test
        @DisplayName("un anillo de llaves no esconde preguntas")
        void cicloNoEsconde() {
            // Un catalogo mal armado donde A depende de B y B de A. Lo seguro es que
            // las dos se muestren, no que las dos desaparezcan.
            VariableCuestionario a = dependiente("B", "1");
            a.setCodigo("A");
            VariableCuestionario b = dependiente("A", "1");
            b.setCodigo("B");
            Map<String, VariableCuestionario> catalogo = Map.of("A", a, "B", b);

            assertTrue(EvaluadorDependencias.habilitada(a, Map.of("A", "1", "B", "1"), catalogo));
            assertTrue(EvaluadorDependencias.habilitada(b, Map.of("A", "1", "B", "1"), catalogo));

            // Con la llave contestada de otra forma si desaparecen, y queda escrito
            // porque la misma condicion se resuelve ahora tambien en la pantalla
            // —habilitacion.ts— y las dos versiones tienen que contestar igual.
            assertFalse(EvaluadorDependencias.habilitada(
                    a, Map.of("A", "2", "B", "2"), catalogo));
        }
    }
}
