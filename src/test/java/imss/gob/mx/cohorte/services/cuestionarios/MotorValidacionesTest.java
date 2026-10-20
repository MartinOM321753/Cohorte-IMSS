package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacion;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.Severidad;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.TipoRegla;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Las comprobaciones que la plantilla declara, sobre los casos reales del papel.
 *
 * <p>Las dos mitades importan igual. Que avise cuando hay algo que revisar, y que
 * <b>calle cuando no</b>: un motor que marca de más enseña a no leer los avisos, y a
 * partir de ahí es como si no existiera.</p>
 */
class MotorValidacionesTest {

    private static VariableCuestionario var(String codigo, TipoVariable tipo, String bloque) {
        VariableCuestionario v = new VariableCuestionario();
        v.setCodigo(codigo);
        v.setTipo(tipo);
        if (bloque != null) {
            SeccionCuestionario s = new SeccionCuestionario();
            s.setNombre(bloque);
            v.setSeccion(s);
        }
        return v;
    }

    private static Map<String, VariableCuestionario> catalogo(VariableCuestionario... vs) {
        Map<String, VariableCuestionario> m = new LinkedHashMap<>();
        for (VariableCuestionario v : vs) m.put(v.getCodigo(), v);
        return m;
    }

    private static ReglaValidacion regla(TipoRegla tipo, String pregunta, String valor,
                                         String compara, Severidad sev) {
        ReglaValidacion r = new ReglaValidacion();
        r.setRegla(tipo);
        r.setPregunta(pregunta);
        r.setValor(valor);
        r.setComparaCon(compara);
        r.setSeveridad(sev);
        r.setMensaje("Hay que revisarlo.");
        r.setActiva(true);
        return r;
    }

    @Nested
    @DisplayName("Marco la casilla de descarte y ademas otra cosa")
    class Excluye {

        private final Map<String, VariableCuestionario> cat = catalogo(
                var("P112_1", TipoVariable.OPCION_UNICA, "P112"),
                var("P112_2", TipoVariable.CASILLA, "P112"),
                var("P112_9", TipoVariable.OPCION_UNICA, "P112"),
                var("P113_1", TipoVariable.OPCION_UNICA, "P113"));

        private final List<ReglaValidacion> reglas = List.of(
                regla(TipoRegla.EXCLUYE, "P112_1", "1", "P112", Severidad.REVISION));

        @Test
        @DisplayName("marco «Ninguna» y ademas una actividad: hay que revisarlo")
        void conflicto() {
            var avisos = MotorValidaciones.revisar(reglas,
                    Map.of("P112_1", "1", "P112_2", "1"), cat, 40);

            assertEquals(1, avisos.size());
            assertEquals("P112_1", avisos.get(0).codigo());
        }

        @Test
        @DisplayName("marco «Ninguna» y nada mas: no hay nada que decir")
        void soloElDescarte() {
            assertTrue(MotorValidaciones.revisar(reglas, Map.of("P112_1", "1"), cat, 40)
                    .isEmpty());
        }

        @Test
        @DisplayName("no marco «Ninguna»: contestar la rejilla es lo normal")
        void sinDescarte() {
            assertTrue(MotorValidaciones.revisar(reglas,
                    Map.of("P112_2", "1", "P112_9", "3"), cat, 40).isEmpty());
        }

        @Test
        @DisplayName("lo contestado en OTRO bloque no cuenta")
        void otroBloqueNoCuenta() {
            // Es lo que hace que la regla sea util y no un estorbo: el descarte apaga
            // su bloque, no el cuestionario entero.
            assertTrue(MotorValidaciones.revisar(reglas,
                    Map.of("P112_1", "1", "P113_1", "2"), cat, 40).isEmpty());
        }

        /**
         * El descarte que excluye solo a una parte de su bloque.
         *
         * <p>La tabla de la 57 son seis productos de tabaco por nueve lugares de
         * compra, y cada renglón empieza con «Yo no compré este producto»: excluye a
         * los ocho lugares <b>de su renglón</b> y a ninguno más. Con el ámbito del
         * bloque, quien no compró cigarros pero sí compró electrónicos en una
         * gasolinera aparecería contradiciéndose, y no se contradice en nada.</p>
         */
        @Nested
        @DisplayName("Un descarte que excluye solo a unas preguntas")
        class PorLista {

            private final Map<String, VariableCuestionario> cat = catalogo(
                    var("P57_01", TipoVariable.CASILLA, "P57"),   // no compro cigarros
                    var("P57_07", TipoVariable.CASILLA, "P57"),   // cigarros, gasolinera
                    var("P57_13", TipoVariable.CASILLA, "P57"),   // cigarros, abarrotes
                    var("P57_02", TipoVariable.CASILLA, "P57"),   // no compro electronicos
                    var("P57_08", TipoVariable.CASILLA, "P57"));  // electronicos, gasolinera

            private final List<ReglaValidacion> reglas = List.of(
                    regla(TipoRegla.EXCLUYE, "P57_01", "1", "P57_07|P57_13",
                            Severidad.REVISION));

            @Test
            @DisplayName("marco el descarte y un lugar de su renglon: hay conflicto")
            void dentroDelRenglon() {
                var avisos = MotorValidaciones.revisar(reglas,
                        Map.of("P57_01", "1", "P57_13", "1"), cat, 40);

                assertEquals(1, avisos.size());
                assertEquals("P57_01", avisos.get(0).codigo());
            }

            @Test
            @DisplayName("un lugar de OTRO renglon del mismo bloque no cuenta")
            void otroRenglonNoCuenta() {
                assertTrue(MotorValidaciones.revisar(reglas,
                                Map.of("P57_01", "1", "P57_08", "1"), cat, 40).isEmpty(),
                        "si esto avisa, la tabla de la 57 se llena de contradicciones "
                                + "que no existen");
            }

            @Test
            @DisplayName("una sola hermana se puede escribir sin la barra")
            void unaSolaHermana() {
                List<ReglaValidacion> sola = List.of(
                        regla(TipoRegla.EXCLUYE, "P57_01", "1", "P57_07",
                                Severidad.REVISION));

                assertEquals(1, MotorValidaciones.revisar(sola,
                        Map.of("P57_01", "1", "P57_07", "1"), cat, 40).size());
                assertTrue(MotorValidaciones.revisar(sola,
                        Map.of("P57_01", "1", "P57_13", "1"), cat, 40).isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("Marco «otro» y no escribio cual")
    class RequiereTexto {

        private final Map<String, VariableCuestionario> cat = catalogo(
                var("P89_6", TipoVariable.CASILLA, "P89"),
                var("P89_7", TipoVariable.TEXTO, "P89"));

        private final List<ReglaValidacion> reglas = List.of(
                regla(TipoRegla.REQUIERE_TEXTO, "P89_6", "1", "P89_7", Severidad.ERROR));

        @Test
        @DisplayName("marcada y sin texto: falta")
        void falta() {
            var avisos = MotorValidaciones.revisar(reglas, Map.of("P89_6", "1"), cat, null);
            assertEquals(1, avisos.size());
            assertEquals(Severidad.ERROR, avisos.get(0).severidad());
        }

        @Test
        @DisplayName("marcada y con texto: bien")
        void completa() {
            assertTrue(MotorValidaciones.revisar(reglas,
                    Map.of("P89_6", "1", "P89_7", "porque si"), cat, null).isEmpty());
        }

        @Test
        @DisplayName("sin marcar no se exige texto")
        void sinMarcar() {
            assertTrue(MotorValidaciones.revisar(reglas, Map.of(), cat, null).isEmpty());
            assertTrue(MotorValidaciones.revisar(reglas, Map.of("P89_6", "2"), cat, null)
                    .isEmpty());
        }

        @Test
        @DisplayName("un espacio en blanco no cuenta como haber escrito")
        void textoEnBlanco() {
            assertEquals(1, MotorValidaciones.revisar(reglas,
                    Map.of("P89_6", "1", "P89_7", "   "), cat, null).size());
        }
    }

    @Nested
    @DisplayName("Contesto una seccion que no le toca por sexo")
    class SoloSexo {

        private final Map<String, VariableCuestionario> cat = catalogo(
                var("P3", TipoVariable.OPCION_UNICA, "P1"),
                var("P209_1", TipoVariable.OPCION_UNICA, "P209"),
                var("P209_2", TipoVariable.TEXTO, "P209"));

        private final List<ReglaValidacion> reglas = List.of(
                regla(TipoRegla.SOLO_SEXO, "P209", "MUJERES", "P3", Severidad.REVISION));

        @Test
        @DisplayName("un hombre contesto la seccion de mujeres")
        void hombreEnSeccionDeMujeres() {
            // P3: 1 = Mujer, 2 = Hombre.
            assertEquals(1, MotorValidaciones.revisar(reglas,
                    Map.of("P3", "2", "P209_1", "1"), cat, null).size());
        }

        @Test
        @DisplayName("una mujer la contesta y no pasa nada")
        void mujerEnSuSeccion() {
            assertTrue(MotorValidaciones.revisar(reglas,
                    Map.of("P3", "1", "P209_1", "1"), cat, null).isEmpty());
        }

        @Test
        @DisplayName("un hombre que la dejo en blanco tampoco da aviso")
        void hombreSinContestarla() {
            // Lo que importa no es a quien se le muestra, sino quien contesto algo.
            assertTrue(MotorValidaciones.revisar(reglas, Map.of("P3", "2"), cat, null)
                    .isEmpty());
        }

        @Test
        @DisplayName("sin saber el sexo, el motor se calla")
        void sinSexo() {
            // Avisar aqui llenaria la pantalla de advertencias que nadie puede
            // resolver, y eso ensena a no leerlas.
            assertTrue(MotorValidaciones.revisar(reglas, Map.of("P209_1", "1"), cat, null)
                    .isEmpty());
        }
    }

    @Nested
    @DisplayName("Fechas y numeros")
    class FechasYNumeros {

        private final Map<String, VariableCuestionario> cat = catalogo(
                var("P1", TipoVariable.FECHA, "P1"),
                var("P8", TipoVariable.NUMERO, "P1"));

        @Test
        @DisplayName("una fecha de manana no puede ser")
        void fechaFutura() {
            var reglas = List.of(regla(TipoRegla.FECHA_NO_FUTURA, "P1", null, null,
                    Severidad.ERROR));
            assertEquals(1, MotorValidaciones.revisar(reglas,
                    Map.of("P1", LocalDate.now().plusDays(1).toString()), cat, null).size());
            assertTrue(MotorValidaciones.revisar(reglas,
                    Map.of("P1", LocalDate.now().minusYears(40).toString()), cat, null)
                    .isEmpty());
        }

        @Test
        @DisplayName("hoy si vale")
        void fechaDeHoy() {
            var reglas = List.of(regla(TipoRegla.FECHA_NO_FUTURA, "P1", null, null,
                    Severidad.ERROR));
            assertTrue(MotorValidaciones.revisar(reglas,
                    Map.of("P1", LocalDate.now().toString()), cat, null).isEmpty());
        }

        @Test
        @DisplayName("lo que no se puede leer como fecha no es asunto de esta regla")
        void fechaIlegible() {
            var reglas = List.of(regla(TipoRegla.FECHA_NO_FUTURA, "P1", null, null,
                    Severidad.ERROR));
            assertTrue(MotorValidaciones.revisar(reglas, Map.of("P1", "marzo"), cat, null)
                    .isEmpty());
        }

        @Test
        @DisplayName("no se puede llevar mas anios viviendo ahi que los que se tienen")
        void masAniosQueLaEdad() {
            var reglas = List.of(regla(TipoRegla.NO_MAYOR_QUE, "P8", null, "EDAD",
                    Severidad.REVISION));
            assertEquals(1, MotorValidaciones.revisar(reglas, Map.of("P8", "50"), cat, 40)
                    .size());
            assertTrue(MotorValidaciones.revisar(reglas, Map.of("P8", "30"), cat, 40)
                    .isEmpty());
            assertTrue(MotorValidaciones.revisar(reglas, Map.of("P8", "40"), cat, 40)
                    .isEmpty(), "justo la edad si vale");
        }

        @Test
        @DisplayName("sin edad no se compara")
        void sinEdad() {
            var reglas = List.of(regla(TipoRegla.NO_MAYOR_QUE, "P8", null, "EDAD",
                    Severidad.REVISION));
            assertTrue(MotorValidaciones.revisar(reglas, Map.of("P8", "50"), cat, null)
                    .isEmpty());
        }
    }

    @Test
    @DisplayName("una regla apagada no se ejecuta")
    void reglaInactiva() {
        ReglaValidacion r = regla(TipoRegla.FECHA_NO_FUTURA, "P1", null, null,
                Severidad.ERROR);
        r.setActiva(false);
        assertTrue(MotorValidaciones.revisar(List.of(r),
                Map.of("P1", LocalDate.now().plusYears(1).toString()),
                catalogo(var("P1", TipoVariable.FECHA, "P1")), null).isEmpty());
    }

    @Test
    @DisplayName("sin reglas no hay avisos, y no truena")
    void sinReglas() {
        assertTrue(MotorValidaciones.revisar(List.of(), Map.of("P1", "x"), Map.of(), 40)
                .isEmpty());
    }
}
