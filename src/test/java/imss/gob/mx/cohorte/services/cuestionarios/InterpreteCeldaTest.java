package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lo que hay que no perder de vista del volcado de Remark.
 *
 * <p>Las pruebas están escritas sobre los casos reales de los archivos recibidos, no
 * sobre casos inventados: el {@code [UNAUTHORIZED]} que cae en el 89% de los campos de
 * texto, el {@code ERROR3100} del bloque P208, el cero que significa dos cosas según el
 * tipo.</p>
 */
class InterpreteCeldaTest {

    private static VariableCuestionario variable(TipoVariable tipo, String... codigos) {
        VariableCuestionario v = new VariableCuestionario();
        v.setCodigo("P1");
        v.setTipo(tipo);
        List<OpcionVariable> opciones = new ArrayList<>();
        for (int i = 0; i < codigos.length; i++) {
            OpcionVariable o = new OpcionVariable();
            o.setCodigo(codigos[i]);
            o.setEtiqueta("Opcion " + codigos[i]);
            o.setOrden(i);
            opciones.add(o);
        }
        v.setOpciones(opciones);
        return v;
    }

    @Nested
    @DisplayName("Las cinco marcas de ausencia se distinguen entre si")
    class Marcas {

        @Test
        @DisplayName("la celda vacia es una respuesta ausente")
        void vacia() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.OPCION_UNICA, "1", "2"), "");
            assertEquals(MarcaAusencia.SIN_RESPUESTA, i.marca());
            assertNull(i.codigo());
        }

        @Test
        @DisplayName("BLANK es lo que escribe Remark cuando declara la region en blanco")
        void enBlanco() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.OPCION_UNICA, "1"), "BLANK");
            assertEquals(MarcaAusencia.EN_BLANCO, i.marca());
        }

        @Test
        @DisplayName("[UNAUTHORIZED] no es una respuesta del participante: es una celda sin leer")
        void noAutorizado() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.TEXTO), "[UNAUTHORIZED]");
            assertEquals(MarcaAusencia.NO_AUTORIZADO, i.marca());
            assertTrue(i.marca().requiereAtencion(),
                    "tiene que salir en la bandeja de recaptura, no contarse como sin contestar");
            assertNull(i.texto(), "no se guarda el literal como si fuera lo que la persona escribio");
            assertEquals("[UNAUTHORIZED]", i.crudo());
        }

        @Test
        @DisplayName("ERROR3100 es un fallo de lectura de la region")
        void errorLectura() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.NUMERO), "ERROR3100");
            assertEquals(MarcaAusencia.ERROR_LECTURA, i.marca());
            assertTrue(i.marca().requiereAtencion());
        }

        @Test
        @DisplayName("la marca se reconoce antes que el tipo, para no reportar el error equivocado")
        void laMarcaVaAntesQueElTipo() {
            // En una variable numerica, un [UNAUTHORIZED] interpretado por tipo saldria
            // como «no es un numero», que manda a buscar el problema al papel en vez de
            // a la licencia de Azure.
            var i = InterpreteCelda.interpretar(variable(TipoVariable.NUMERO), "[UNAUTHORIZED]");
            assertEquals(MarcaAusencia.NO_AUTORIZADO, i.marca());
            assertFalse(i.tieneProblema());
        }
    }

    @Nested
    @DisplayName("El cero significa cosas distintas segun el tipo")
    class Cero {

        @Test
        @DisplayName("en una casilla es ausencia de marca")
        void enCasilla() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.CASILLA, "1"), "0");
            assertEquals(MarcaAusencia.CERO_MARCAS, i.marca());
        }

        @Test
        @DisplayName("en un numero es la cantidad cero, que es una respuesta valida")
        void enNumero() {
            VariableCuestionario v = variable(TipoVariable.NUMERO);
            v.setValorMin(0d);
            v.setValorMax(99d);
            var i = InterpreteCelda.interpretar(v, "0");
            assertEquals(MarcaAusencia.NINGUNA, i.marca());
            assertEquals(0d, i.numero());
        }

        @Test
        @DisplayName("en una opcion unica cuyo catalogo incluye el 0, es esa opcion")
        void enOpcionConCeroEnElCatalogo() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.OPCION_UNICA, "0", "1"), "0");
            assertEquals(MarcaAusencia.NINGUNA, i.marca());
            assertEquals("0", i.codigo());
        }

        @Test
        @DisplayName("en una opcion unica sin 0 en el catalogo, son cero marcas")
        void enOpcionSinCeroEnElCatalogo() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.OPCION_UNICA, "1", "2"), "0");
            assertEquals(MarcaAusencia.CERO_MARCAS, i.marca());
        }
    }

    @Nested
    @DisplayName("Un valor que no encaja se guarda senalado, no se descarta")
    class Problemas {

        @Test
        @DisplayName("un codigo fuera del catalogo se conserva con su problema")
        void codigoFueraDelCatalogo() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.OPCION_UNICA, "1", "2"), "7");
            assertEquals(MarcaAusencia.NINGUNA, i.marca());
            assertEquals("7", i.codigo(), "el dato llega de un escaner: perderlo seria peor");
            assertTrue(i.tieneProblema());
            assertEquals("7", i.crudo());
        }

        @Test
        @DisplayName("sin catalogo no hay nada contra lo que comprobar, y se acepta")
        void sinCatalogo() {
            // Es el caso de las 60 variables que el volcado produce y ningun catalogo
            // documenta: P52_1..P52_56 y P159_3..P159_6.
            var i = InterpreteCelda.interpretar(variable(TipoVariable.OPCION_UNICA), "3");
            assertEquals("3", i.codigo());
            assertFalse(i.tieneProblema());
        }

        @Test
        @DisplayName("un numero fuera de rango se guarda y se senala")
        void fueraDeRango() {
            VariableCuestionario v = variable(TipoVariable.NUMERO);
            v.setValorMax(12d);
            var i = InterpreteCelda.interpretar(v, "40");
            assertEquals(40d, i.numero());
            assertTrue(i.tieneProblema());
        }
    }

    @Nested
    @DisplayName("Casos del formato")
    class Formato {

        @Test
        @DisplayName("un codigo que paso por una celda numerica llega como 7.0 y sigue siendo el 7")
        void codigoConDecimalDeExcel() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.OPCION_UNICA, "7"), "7.0");
            assertEquals("7", i.codigo());
            assertFalse(i.tieneProblema());
        }

        @Test
        @DisplayName("el escape de un numero se reconoce por el catalogo, no por ser alto")
        void escapePorCatalogo() {
            VariableCuestionario v = variable(TipoVariable.NUMERO_CON_ESCAPE, "99");
            v.setValorMax(60d);

            var escape = InterpreteCelda.interpretar(v, "99");
            assertEquals("99", escape.codigo());
            assertNull(escape.numero(), "el escape no es una cantidad");

            var alto = InterpreteCelda.interpretar(v, "80");
            assertEquals(80d, alto.numero(), "un valor alto que no es el escape sigue siendo cantidad");
            assertTrue(alto.tieneProblema());
        }

        @Test
        @DisplayName("la fecha se lee como viene el volcado: con guiones, con barras y en ISO")
        void fechas() {
            VariableCuestionario v = variable(TipoVariable.FECHA);
            // ISO: lo que el lector entrega para una celda con formato de fecha de Excel.
            assertEquals(LocalDate.of(1970, 3, 12),
                    InterpreteCelda.interpretar(v, "1970-03-12").fecha());
            assertEquals(LocalDate.of(1970, 3, 12),
                    InterpreteCelda.interpretar(v, "1970-03-12T00:00").fecha());
            assertEquals(LocalDate.of(1970, 3, 12),
                    InterpreteCelda.interpretar(v, "12/03/1970").fecha());

            // Con guiones: es como Remark manda P1 «Fecha de Nacimiento» en los volcados
            // recibidos, y durante un tiempo se rechazaba —fechas buenas que obligaban a
            // recapturar a mano—. Esta linea es la prueba de que no vuelva a pasar.
            assertEquals(LocalDate.of(1968, 6, 13),
                    InterpreteCelda.interpretar(v, "13-06-1968").fecha());
            assertEquals(LocalDate.of(1962, 7, 31),
                    InterpreteCelda.interpretar(v, "31-07-1962").fecha());
        }

        @Test
        @DisplayName("lo que no es una fecha se senala, y no se adivina el mes ni el siglo")
        void fechasQueNoLoSon() {
            VariableCuestionario v = variable(TipoVariable.FECHA);
            assertTrue(InterpreteCelda.interpretar(v, "marzo de 1970").tieneProblema());

            // El papel pide dia, mes y ano en ese orden, asi que esto no es «otra
            // convencion»: es un mes 13. Leerlo al reves lo daria por bueno cambiado de
            // mes, y nadie se enteraria.
            assertTrue(InterpreteCelda.interpretar(v, "06-13-1968").tieneProblema());

            // Ano de dos cifras: 68 puede ser 1968 o 2068, y en una cohorte eso no se
            // adivina.
            assertTrue(InterpreteCelda.interpretar(v, "13-06-68").tieneProblema());

            // El valor literal se conserva siempre: es lo unico que permite recuperarlo.
            assertEquals("06-13-1968", InterpreteCelda.interpretar(v, "06-13-1968").crudo());
        }

        @Test
        @DisplayName("un encabezado de grupo con valor delata un error del catalogo")
        void grupoConValor() {
            // P73 «te o infusion» y P75 «salsas y aderezos» son encabezados: en el papel
            // no tienen casillas. Si el volcado trae algo, el catalogo esta mal.
            var i = InterpreteCelda.interpretar(variable(TipoVariable.GRUPO), "1");
            assertTrue(i.tieneProblema());
            assertEquals("1", i.texto());
        }
    }

    @Nested
    @DisplayName("La celda vacia no significa lo mismo en una casilla que en una opcion")
    class Vacia {

        // Es la diferencia entre «falta el dato» y «ese es el dato». Quien no nada deja
        // vacia la casilla «Nadar Lunes»: la casilla vacia ES la respuesta. Contarla
        // como sin responder inflaba los faltantes de forma absurda —un participante
        // que hace tres actividades deja casi cien casillas vacias solo en la 112—.

        @Test
        @DisplayName("casilla vacia: no la marco")
        void casillaVacia() {
            var v = variable(TipoVariable.CASILLA, "1");
            assertEquals(MarcaAusencia.CERO_MARCAS, InterpreteCelda.interpretar(v, "").marca());
            assertEquals(MarcaAusencia.CERO_MARCAS, InterpreteCelda.interpretar(v, "   ").marca());
            assertEquals(MarcaAusencia.CERO_MARCAS, InterpreteCelda.interpretar(v, null).marca());
        }

        @Test
        @DisplayName("una casilla vacia y una con cero se leen igual, porque son lo mismo")
        void ceroYVacio() {
            var v = variable(TipoVariable.CASILLA, "1");
            assertEquals(InterpreteCelda.interpretar(v, "0").marca(),
                    InterpreteCelda.interpretar(v, "").marca());
        }

        @Test
        @DisplayName("la casilla de un grupo multiple se trata igual")
        void opcionMultiple() {
            assertEquals(MarcaAusencia.CERO_MARCAS,
                    InterpreteCelda.interpretar(variable(TipoVariable.OPCION_MULTIPLE, "1"), "").marca());
        }

        @Test
        @DisplayName("en lo demas, vacio sigue siendo sin respuesta")
        void losDemasTipos() {
            // Aqui si falta el dato: una pregunta de opciones sin marcar es una pregunta
            // que nadie contesto, y tiene que seguir contandose como tal.
            for (TipoVariable tipo : new TipoVariable[]{
                    TipoVariable.OPCION_UNICA, TipoVariable.NUMERO,
                    TipoVariable.NUMERO_CON_ESCAPE, TipoVariable.TEXTO, TipoVariable.FECHA}) {
                assertEquals(MarcaAusencia.SIN_RESPUESTA,
                        InterpreteCelda.interpretar(variable(tipo, "1", "2"), "").marca(),
                        "el tipo " + tipo + " no deberia leerse como cero marcas");
            }
        }

        @Test
        @DisplayName("la casilla vacia sigue sin traer valor, solo cambia como se llama")
        void sinValor() {
            var i = InterpreteCelda.interpretar(variable(TipoVariable.CASILLA, "1"), "");
            assertNull(i.codigo());
            assertNull(i.numero());
            assertFalse(i.tieneProblema());
        }

        @Test
        @DisplayName("y cuenta como contestada, que es de lo que se trataba")
        void cuentaComoContestada() {
            // Sin esto el cambio seria solo de nombre. Lo que importa es que una rejilla
            // de 105 casillas con cuatro marcadas no quede con 101 pendientes, y que el
            // cuestionario se pueda cerrar.
            var casilla = respuestaDe(TipoVariable.CASILLA, "");
            assertFalse(casilla.tieneValor(), "no tiene valor, eso no cambia");
            assertTrue(casilla.cuentaComoContestada(), "pero la respuesta es «no la marco»");

            // En una pregunta de opciones, cero marcas si es un hueco: alguien tenia que
            // elegir una de las opciones y no eligio ninguna.
            assertFalse(respuestaDe(TipoVariable.OPCION_UNICA, "0").cuentaComoContestada());
        }

        private RespuestaCuestionario respuestaDe(TipoVariable tipo, String celda) {
            VariableCuestionario v = variable(tipo, "1", "2");
            var i = InterpreteCelda.interpretar(v, celda);
            RespuestaCuestionario r = new RespuestaCuestionario();
            r.setVariable(v);
            r.setMarcaAusencia(i.marca());
            r.setValorCodigo(i.codigo());
            return r;
        }
    }
}
