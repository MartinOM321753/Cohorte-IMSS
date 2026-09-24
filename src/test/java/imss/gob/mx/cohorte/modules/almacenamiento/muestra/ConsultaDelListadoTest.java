package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import jakarta.persistence.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * El JPQL del listado se arma en tiempo de ejecución, así que nadie lo revisa al
 * compilar.
 *
 * <p>El fallo característico de una consulta dinámica es que la cláusula y el
 * enlace del parámetro dejen de ir juntos: se agrega un filtro y se olvida
 * enlazarlo —o al revés, se enlaza uno que ninguna cláusula menciona— y
 * Hibernate solo protesta cuando alguien abre la pantalla. Aquí se comprueba
 * esa correspondencia para todas las combinaciones de criterios, sin base de
 * datos de por medio.</p>
 */
class ConsultaDelListadoTest {

    private static final Pattern PARAMETRO = Pattern.compile(":([a-zA-Z][a-zA-Z0-9]*)");

    /** Los parámetros que el texto de la consulta menciona. */
    private static Set<String> mencionados(String jpql) {
        Set<String> nombres = new LinkedHashSet<>();
        Matcher m = PARAMETRO.matcher(jpql);
        while (m.find()) {
            nombres.add(m.group(1));
        }
        return nombres;
    }

    /** Los parámetros que el enlazador realmente asigna. */
    private static Set<String> enlazados(CriteriosMuestra criterios) {
        Query q = mock(Query.class);
        when(q.setParameter(anyString(), any())).thenReturn(q);
        MuestraCursorRepositoryImpl.enlazar(q, criterios);

        ArgumentCaptor<String> nombres = ArgumentCaptor.forClass(String.class);
        verify(q, atLeastOnce()).setParameter(nombres.capture(), any());
        return new LinkedHashSet<>(nombres.getAllValues());
    }

    private static CriteriosMuestra criterios(boolean historico, boolean ocultar, String busqueda,
                                              LocalDate desde, LocalDate hasta, List<String> tipos,
                                              String sexo, String folioDesde, String folioHasta) {
        return CriteriosMuestra.de(7L, historico, ocultar, busqueda, desde, hasta,
                tipos, sexo, folioDesde, folioHasta);
    }

    private static CriteriosMuestra sinFiltros() {
        return criterios(false, true, null, null, null, null, null, null, null);
    }

    /**
     * Las 2^9 combinaciones de criterios encendidos y apagados.
     *
     * <p>Se recorren todas porque el error no está en un filtro concreto sino en
     * la interacción: una cláusula que solo aparece cuando coinciden dos
     * criterios, o un paréntesis que solo se desequilibra cuando falta uno.</p>
     */
    private static List<CriteriosMuestra> todasLasCombinaciones() {
        List<CriteriosMuestra> todas = new ArrayList<>();
        for (int mascara = 0; mascara < 512; mascara++) {
            todas.add(criterios(
                    (mascara & 1) != 0,
                    (mascara & 2) != 0,
                    (mascara & 4) != 0 ? "heces" : null,
                    (mascara & 8) != 0 ? LocalDate.of(2026, 1, 1) : null,
                    (mascara & 16) != 0 ? LocalDate.of(2026, 12, 31) : null,
                    (mascara & 32) != 0 ? List.of("Heces", "Sangre") : null,
                    (mascara & 64) != 0 ? "F" : null,
                    (mascara & 128) != 0 ? "1" : null,
                    (mascara & 256) != 0 ? "500" : null));
        }
        return todas;
    }

    private static long cuenta(String texto, char caracter) {
        return texto.chars().filter(c -> c == caracter).count();
    }

    // ── Correspondencia entre cláusulas y parámetros ─────────────────────────

    @Nested
    @DisplayName("Cada cláusula construida tiene su parámetro enlazado")
    class Correspondencia {

        @Test
        @DisplayName("La consulta de la página, en todas las combinaciones de filtros")
        void laPagina() {
            for (CriteriosMuestra c : todasLasCombinaciones()) {
                for (boolean conCursor : new boolean[]{false, true}) {
                    for (boolean haciaAtras : new boolean[]{false, true}) {
                        String jpql = MuestraCursorRepositoryImpl.jpqlPagina(c, conCursor, haciaAtras);

                        Set<String> esperados = new LinkedHashSet<>(enlazados(c));
                        if (conCursor) {
                            // Estos dos no salen del enlazador: los pone la propia
                            // paginación, que es quien conoce la frontera.
                            esperados.add("curFecha");
                            esperados.add("curId");
                        }

                        assertEquals(esperados, mencionados(jpql),
                                "no coinciden los parámetros con " + c);
                    }
                }
            }
        }

        @Test
        @DisplayName("El conteo usa exactamente los mismos parámetros que la página")
        void elConteo() {
            for (CriteriosMuestra c : todasLasCombinaciones()) {
                assertEquals(enlazados(c), mencionados(MuestraCursorRepositoryImpl.jpqlConteo(c)),
                        "no coinciden los parámetros con " + c);
            }
        }

        @Test
        @DisplayName("El conteo de huérfanas devueltas también")
        void lasHuerfanas() {
            for (CriteriosMuestra c : todasLasCombinaciones()) {
                assertEquals(enlazados(c), mencionados(MuestraCursorRepositoryImpl.jpqlHuerfanas(c)),
                        "no coinciden los parámetros con " + c);
            }
        }

        @Test
        @DisplayName("La consulta de alícuotas solo pide la institución y los padres")
        void lasAlicuotas() {
            for (CriteriosMuestra c : todasLasCombinaciones()) {
                assertEquals(Set.of("idsPadre", "idInst"),
                        mencionados(MuestraCursorRepositoryImpl.jpqlAlicuotas(c)),
                        "las alícuotas no deben arrastrar los filtros de la pantalla");
            }
        }
    }

    // ── Forma de la consulta ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Forma de la consulta")
    class Forma {

        @Test
        @DisplayName("Los paréntesis cierran en todas las combinaciones")
        void parentesisEquilibrados() {
            for (CriteriosMuestra c : todasLasCombinaciones()) {
                for (String jpql : List.of(
                        MuestraCursorRepositoryImpl.jpqlPagina(c, true, false),
                        MuestraCursorRepositoryImpl.jpqlPagina(c, true, true),
                        MuestraCursorRepositoryImpl.jpqlConteo(c),
                        MuestraCursorRepositoryImpl.jpqlHuerfanas(c),
                        MuestraCursorRepositoryImpl.jpqlAlicuotas(c))) {
                    assertEquals(cuenta(jpql, '('), cuenta(jpql, ')'),
                            "paréntesis descompensados: " + jpql);
                }
            }
        }

        @Test
        @DisplayName("Nunca queda un AND o un OR colgando")
        void sinConectoresHuerfanos() {
            for (CriteriosMuestra c : todasLasCombinaciones()) {
                String jpql = MuestraCursorRepositoryImpl.jpqlPagina(c, true, false);
                assertFalse(jpql.contains("AND ()"), "condición vacía entre paréntesis: " + jpql);
                assertFalse(jpql.contains("AND AND"), jpql);
                assertFalse(jpql.contains("OR OR"), jpql);
                assertFalse(jpql.contains("AND  OR"), jpql);
                assertFalse(jpql.contains("WHERE AND"), jpql);
                assertFalse(jpql.contains("AND ORDER BY"), jpql);
            }
        }

        @Test
        @DisplayName("El orden se invierte al pedir hacia lo más reciente")
        void ordenSegunLaDireccion() {
            CriteriosMuestra c = sinFiltros();
            assertTrue(MuestraCursorRepositoryImpl.jpqlPagina(c, true, false)
                    .endsWith("ORDER BY m.fechaRegistro DESC, m.id DESC"));
            assertTrue(MuestraCursorRepositoryImpl.jpqlPagina(c, true, true)
                    .endsWith("ORDER BY m.fechaRegistro ASC, m.id ASC"));
        }

        @Test
        @DisplayName("El cursor compara por el par (fecha, id), no solo por la fecha")
        void elCursorDesempataPorId() {
            String haciaAdelante = MuestraCursorRepositoryImpl.jpqlPagina(sinFiltros(), true, false);
            assertTrue(haciaAdelante.contains("m.fechaRegistro < :curFecha"));
            assertTrue(haciaAdelante.contains("m.fechaRegistro = :curFecha AND m.id < :curId"));

            String haciaAtras = MuestraCursorRepositoryImpl.jpqlPagina(sinFiltros(), true, true);
            assertTrue(haciaAtras.contains("m.fechaRegistro > :curFecha"));
            assertTrue(haciaAtras.contains("m.fechaRegistro = :curFecha AND m.id > :curId"));
        }

        @Test
        @DisplayName("Sin cursor no hay comparación de frontera: es la primera página")
        void sinCursorNoHayFrontera() {
            String jpql = MuestraCursorRepositoryImpl.jpqlPagina(sinFiltros(), false, false);
            assertFalse(jpql.contains("curFecha"));
            assertFalse(jpql.contains("curId"));
        }

        @Test
        @DisplayName("El padre se une por fuera para no perder las muestras sin padre")
        void elPadreSeUneConLeftJoin() {
            String jpql = MuestraCursorRepositoryImpl.jpqlPagina(sinFiltros(), false, false);
            assertTrue(jpql.contains("LEFT JOIN m.muestraPadre pad"),
                    "una unión interna borraría del listado a todas las muestras primarias");
            assertTrue(jpql.contains("LEFT JOIN pad.institucion padInst"));
            assertTrue(jpql.contains("pad IS NULL OR NOT"));
        }

        @Test
        @DisplayName("El histórico solo aparece cuando se pide")
        void elHistoricoEsOpcional() {
            assertFalse(MuestraCursorRepositoryImpl.jpqlPagina(sinFiltros(), false, false)
                    .contains("TrasladoMuestra"));

            CriteriosMuestra conHistorico =
                    criterios(true, true, null, null, null, null, null, null, null);
            assertTrue(MuestraCursorRepositoryImpl.jpqlPagina(conHistorico, false, false)
                    .contains("TrasladoMuestra"));
        }

        @Test
        @DisplayName("Fecha y tipo se juzgan también por las alícuotas; sexo y folio no")
        void laDisyuncionSoloCubreLoQuePuedeDiferir() {
            CriteriosMuestra c = criterios(false, true, null,
                    LocalDate.of(2026, 1, 1), null, List.of("Heces"), "F", "1", "9");
            String jpql = MuestraCursorRepositoryImpl.jpqlConteo(c);

            assertTrue(jpql.contains("EXISTS (SELECT 1 FROM Muestra b"),
                    "el tipo del lote puede no ser el de la padre");
            assertTrue(jpql.contains("b.fechaRecoleccion >= :desde"));
            assertTrue(jpql.contains("LOWER(btip.nombre) IN :tipos"));

            // El participante es el mismo en toda la descendencia: repetir estos
            // dos dentro del EXISTS solo costaría trabajo a la base.
            assertFalse(jpql.contains("b.paciente"));
            assertEquals(1, cuenta(jpql.replace("per.sexo = :sexo", " "), ' '));
        }
    }

    // ── Normalización de los criterios ───────────────────────────────────────

    @Nested
    @DisplayName("Normalización de los criterios")
    class Normalizacion {

        @Test
        @DisplayName("El folio tecleado se rellena como está almacenado")
        void elFolioSeRellena() {
            CriteriosMuestra c = criterios(false, true, null, null, null, null, null, "1", "502");
            assertEquals("000001", c.folioDesde());
            assertEquals("000502", c.folioHasta());
        }

        @Test
        @DisplayName("Como texto, «1000000» es MENOR que «999999»: de ahí el ancho fijo")
        void elOrdenDeTextoSoloValeAIgualLongitud() {
            // La razón de que el rango se acote a folios de seis dígitos y de que
            // la consulta lo exija explícitamente. Comparar longitudes distintas
            // como texto daría un orden que no es el numérico.
            assertTrue("1000000".compareTo("999999") < 0);
            assertTrue("000502".compareTo("999999") < 0);
        }

        @Test
        @DisplayName("Un folio por encima del ancho se recorta al máximo que puede existir")
        void elFolioLargoSeRecortaAlMaximo() {
            CriteriosMuestra c = criterios(false, true, null, null, null, null, null,
                    "000502", "1000000");
            assertEquals("000502", c.folioDesde());
            assertEquals("999999", c.folioHasta());
        }

        @Test
        @DisplayName("Tecleando un solo extremo, el otro se fija igual")
        void losExtremosVanSiempreJuntos() {
            CriteriosMuestra soloDesde =
                    criterios(false, true, null, null, null, null, null, "300", null);
            assertEquals("000300", soloDesde.folioDesde());
            assertEquals("999999", soloDesde.folioHasta());

            CriteriosMuestra soloHasta =
                    criterios(false, true, null, null, null, null, null, null, "300");
            assertEquals("000000", soloHasta.folioDesde());
            assertEquals("000300", soloHasta.folioHasta());
        }

        @Test
        @DisplayName("Pedir desde un folio que no puede existir no devuelve nada")
        void elRangoImposibleQuedaVacio() {
            CriteriosMuestra c = criterios(false, true, null, null, null, null, null,
                    "5000000", null);
            assertTrue(c.folioDesde().compareTo(c.folioHasta()) > 0,
                    "un rango invertido es la forma natural de no devolver filas");
        }

        @Test
        @DisplayName("La consulta exige el ancho, no solo el rango")
        void laConsultaExigeElAncho() {
            CriteriosMuestra c = criterios(false, true, null, null, null, null, null, "1", "9");
            String jpql = MuestraCursorRepositoryImpl.jpqlConteo(c);
            assertTrue(jpql.contains("LENGTH(pac.folio) = 6"), jpql);
            assertTrue(jpql.contains("pac.folio >= :folioDesde"));
            assertTrue(jpql.contains("pac.folio <= :folioHasta"));
        }

        @Test
        @DisplayName("Un folio sin dígitos no acota nada en vez de vaciar la lista")
        void elFolioSinDigitosSeIgnora() {
            CriteriosMuestra c = criterios(false, true, null, null, null, null, null, "  ", "abc");
            assertNull(c.folioDesde());
            assertNull(c.folioHasta());
        }

        @Test
        @DisplayName("La fecha final llega hasta el último instante del día")
        void laFechaFinalEsInclusiva() {
            CriteriosMuestra c = criterios(false, true, null, null,
                    LocalDate.of(2026, 9, 20), null, null, null, null);
            assertEquals(2026, c.hasta().getYear());
            assertEquals(23, c.hasta().getHour());
            assertEquals(59, c.hasta().getMinute());
        }

        @Test
        @DisplayName("Los tipos llegan en minúsculas y sin repetir")
        void losTiposSeNormalizan() {
            CriteriosMuestra c = criterios(false, true, null, null, null,
                    List.of("Heces", " heces ", "Sangre"), null, null, null);
            assertEquals(List.of("heces", "sangre"), c.tipos());
        }

        @Test
        @DisplayName("Una búsqueda en blanco no es una búsqueda")
        void laBusquedaVaciaNoFiltra() {
            assertFalse(criterios(false, true, "   ", null, null, null, null, null, null)
                    .hayBusqueda());
        }

        @Test
        @DisplayName("Un sexo desconocido se ignora en vez de dejar la lista vacía")
        void elSexoDesconocidoSeIgnora() {
            assertNull(criterios(false, true, null, null, null, null, "X", null, null).sexo());
        }
    }

    // ── El cursor ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("El cursor")
    class Cursor {

        @Test
        @DisplayName("Ida y vuelta conserva el par exacto")
        void idaYVuelta() {
            CursorMuestra original = new CursorMuestra(1_758_400_000_000L, 502L);
            CursorMuestra vuelta = CursorMuestra.decodificar(original.codificar());
            assertEquals(original, vuelta);
        }

        @Test
        @DisplayName("Un cursor ilegible devuelve la primera página en vez de reventar")
        void elCursorCorruptoNoRompe() {
            assertNull(CursorMuestra.decodificar("no-es-un-cursor!!"));
            assertNull(CursorMuestra.decodificar(""));
            assertNull(CursorMuestra.decodificar(null));
            // Base64 válido, contenido que no lo es.
            assertNull(CursorMuestra.decodificar(
                    java.util.Base64.getUrlEncoder().withoutPadding()
                            .encodeToString("hola".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        }

        @Test
        @DisplayName("No lleva caracteres que haya que escapar en una URL")
        void viajaLimpioEnLaUrl() {
            String codificado = new CursorMuestra(1_758_400_000_000L, 502L).codificar();
            assertTrue(codificado.matches("[A-Za-z0-9_-]+"), codificado);
        }
    }
}
