package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.tipos.DirigidoA;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A quién va dirigido cada instrumento.
 *
 * <p>Los casos están tomados de los instrumentos reales de la cohorte: el minimental y la
 * afluencia verbal se aplican de los 45 años en adelante, y el cuestionario de mujeres
 * lleva una hoja que los demás no tienen.</p>
 */
class CriterioAplicabilidadTest {

    private static TipoCuestionario tipo(DirigidoA dirigido, Integer min, Integer max) {
        TipoCuestionario t = new TipoCuestionario();
        t.setClave("C2");
        t.setNombre("Minimental");
        t.setDirigidoA(dirigido);
        t.setEdadMinima(min);
        t.setEdadMaxima(max);
        return t;
    }

    private static Paciente participante(Persona.Sexo sexo, LocalDate nacimiento) {
        Persona p = new Persona();
        p.setSexo(sexo);
        p.setFechaNacimiento(nacimiento);
        Paciente pac = new Paciente();
        pac.setFolio("000502");
        pac.setPersona(p);
        return pac;
    }

    @Nested
    @DisplayName("Sexo")
    class Sexo {

        @Test
        @DisplayName("sin restriccion, entra cualquiera")
        void ambos() {
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.AMBOS, null, null),
                    participante(Persona.Sexo.M, LocalDate.of(1970, 1, 1)), null);
            assertTrue(r.aplica());
            assertFalse(r.indeterminado());
            assertNull(r.motivo());
        }

        @Test
        @DisplayName("un instrumento de mujeres no le toca a un hombre")
        void soloMujeres() {
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.MUJERES, null, null),
                    participante(Persona.Sexo.M, LocalDate.of(1970, 1, 1)), null);
            assertFalse(r.aplica());
            assertFalse(r.indeterminado(), "se sabe de sobra que no le toca");
            assertTrue(r.motivo().contains("mujeres"));
        }

        @Test
        @DisplayName("un sexo sin registrar no decide: queda indeterminado")
        void sexoSinRegistrar() {
            // El hueco es del expediente, no del participante. Tratarlo como «no aplica»
            // lo dejaria sin poder contestar nada por un dato que nadie capturo.
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.MUJERES, null, null),
                    participante(null, LocalDate.of(1970, 1, 1)), null);
            assertFalse(r.aplica());
            assertTrue(r.indeterminado());
            assertTrue(r.motivo().contains("no tiene sexo registrado"));
        }

        @Test
        @DisplayName("sin restriccion de sexo, que falte el dato da igual")
        void sexoSinRegistrarPeroSinRestriccion() {
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.AMBOS, null, null),
                    participante(null, LocalDate.of(1970, 1, 1)), null);
            assertTrue(r.aplica());
            assertFalse(r.indeterminado());
        }
    }

    @Nested
    @DisplayName("Edad")
    class Edad {

        @Test
        @DisplayName("el minimental de 45 en adelante excluye a quien tenia 44")
        void porDebajoDelMinimo() {
            var minimental = tipo(DirigidoA.AMBOS, 45, null);
            var r = CriterioAplicabilidad.evaluar(minimental,
                    participante(Persona.Sexo.F, LocalDate.of(1980, 6, 1)),
                    LocalDate.of(2024, 6, 1));   // cumple 44 ese mismo dia
            assertFalse(r.aplica());
            assertEquals(44, r.edad());
        }

        @Test
        @DisplayName("el dia en que cumple el minimo, ya entra")
        void justoEnElMinimo() {
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.AMBOS, 45, null),
                    participante(Persona.Sexo.F, LocalDate.of(1979, 6, 1)),
                    LocalDate.of(2024, 6, 1));
            assertTrue(r.aplica());
            assertEquals(45, r.edad());
        }

        @Test
        @DisplayName("el tope es inclusivo: 49 entra en «18 a 49»")
        void justoEnElMaximo() {
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.MUJERES, 18, 49),
                    participante(Persona.Sexo.F, LocalDate.of(1975, 1, 1)),
                    LocalDate.of(2024, 6, 1));
            assertTrue(r.aplica());
            assertEquals(49, r.edad());
        }

        @Test
        @DisplayName("una edad exacta se expresa con minimo y maximo iguales")
        void edadExacta() {
            var t = tipo(DirigidoA.AMBOS, 60, 60);
            assertTrue(CriterioAplicabilidad.evaluar(t,
                    participante(Persona.Sexo.M, LocalDate.of(1964, 6, 1)),
                    LocalDate.of(2024, 6, 1)).aplica());
            assertFalse(CriterioAplicabilidad.evaluar(t,
                    participante(Persona.Sexo.M, LocalDate.of(1963, 6, 1)),
                    LocalDate.of(2024, 6, 1)).aplica());
            assertTrue(CriterioAplicabilidad.rangoComoTexto(t).contains("exactamente 60"));
        }

        @Test
        @DisplayName("sin fecha de nacimiento y con restriccion de edad: indeterminado")
        void sinFechaDeNacimiento() {
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.AMBOS, 45, null),
                    participante(Persona.Sexo.M, null), null);
            assertFalse(r.aplica());
            assertTrue(r.indeterminado());
            assertNull(r.edad());
        }

        @Test
        @DisplayName("una fecha de nacimiento posterior a la aplicacion es un dato mal capturado")
        void nacimientoEnElFuturo() {
            // No se devuelve una edad negativa ni se da por valido: se senala el dato.
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.AMBOS, 45, null),
                    participante(Persona.Sexo.M, LocalDate.of(2030, 1, 1)),
                    LocalDate.of(2024, 6, 1));
            assertTrue(r.indeterminado());
            assertNull(r.edad());
            assertTrue(r.motivo().contains("posterior"));
        }
    }

    @Nested
    @DisplayName("La edad se mide el dia de la aplicacion, no hoy")
    class FechaDeReferencia {

        @Test
        @DisplayName("un error de aplicacion no se borra porque el participante haya cumplido anos")
        void elErrorNoCaduca() {
            // Contesto el minimental a los 44, por error. Dos anos despues tiene 46: si se
            // evaluara con la fecha de hoy, el error desapareceria del registro.
            var minimental = tipo(DirigidoA.AMBOS, 45, null);
            var persona = participante(Persona.Sexo.F, LocalDate.of(1980, 1, 1));

            var alAplicar = CriterioAplicabilidad.evaluar(minimental, persona, LocalDate.of(2024, 1, 1));
            assertFalse(alAplicar.aplica(), "a los 44 no le tocaba");
            assertEquals(44, alAplicar.edad());

            var dosAnosDespues = CriterioAplicabilidad.evaluar(minimental, persona, LocalDate.of(2026, 1, 1));
            assertTrue(dosAnosDespues.aplica(), "a los 46 si le tocaria");
            assertEquals(46, dosAnosDespues.edad());
        }

        @Test
        @DisplayName("y al reves: un tope de edad no invalida lo que fue correcto")
        void loCorrectoSigueSiendoloMirandoLaFechaDeEntonces() {
            var soloJovenes = tipo(DirigidoA.AMBOS, null, 45);
            var persona = participante(Persona.Sexo.M, LocalDate.of(1979, 1, 1));
            assertTrue(CriterioAplicabilidad.evaluar(soloJovenes, persona, LocalDate.of(2024, 1, 1)).aplica());
            assertFalse(CriterioAplicabilidad.evaluar(soloJovenes, persona, LocalDate.of(2030, 1, 1)).aplica());
        }
    }

    @Nested
    @DisplayName("El publico, en una frase")
    class Publico {

        @Test
        @DisplayName("se arma segun lo que este configurado")
        void frases() {
            assertEquals("Todos los participantes",
                    CriterioAplicabilidad.publicoComoTexto(tipo(DirigidoA.AMBOS, null, null)));
            assertEquals("Solo mujeres",
                    CriterioAplicabilidad.publicoComoTexto(tipo(DirigidoA.MUJERES, null, null)));
            assertEquals("De 45 anos en adelante",
                    CriterioAplicabilidad.publicoComoTexto(tipo(DirigidoA.AMBOS, 45, null)));
            assertEquals("Mujeres de 18 a 49 anos",
                    CriterioAplicabilidad.publicoComoTexto(tipo(DirigidoA.MUJERES, 18, 49)));
            assertEquals("Hasta 17 anos",
                    CriterioAplicabilidad.publicoComoTexto(tipo(DirigidoA.AMBOS, null, 17)));
        }
    }

    @Nested
    @DisplayName("Los dos criterios se acumulan")
    class Combinado {

        @Test
        @DisplayName("fallar en los dos los reporta los dos")
        void ambosMotivos() {
            var r = CriterioAplicabilidad.evaluar(tipo(DirigidoA.MUJERES, 45, null),
                    participante(Persona.Sexo.M, LocalDate.of(1990, 1, 1)),
                    LocalDate.of(2024, 1, 1));
            assertFalse(r.aplica());
            assertEquals(2, r.motivos().size(),
                    "no basta con decir el primero: quien lo lea tiene que ver todo lo que no cuadra");
        }
    }
}
