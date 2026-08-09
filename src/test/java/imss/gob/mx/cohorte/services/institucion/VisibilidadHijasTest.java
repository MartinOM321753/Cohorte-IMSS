package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.*;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Comprobaciones de la visibilidad configurable de hijas. Todo con dobles: lo que
 * se verifica es la regla, no el mapeo.
 *
 * <p>Árbol de las pruebas:  1 → {2, 3};  2 → {4}.</p>
 */
class VisibilidadHijasTest {

    private InstitucionRepository institucionRepository;
    private VisibilidadInstitucionHijaRepository visibilidadRepository;
    private InstitucionContextService contexto;
    private InstitucionArbolService arbol;
    private InstitucionVisibilidadService visibilidad;

    private final Map<Long, Institucion> instituciones = new HashMap<>();

    @BeforeEach
    void setUp() {
        institucionRepository = mock(InstitucionRepository.class);
        visibilidadRepository = mock(VisibilidadInstitucionHijaRepository.class);
        contexto = mock(InstitucionContextService.class);

        crear(1L, null);
        crear(2L, 1L);
        crear(3L, 1L);
        crear(4L, 2L);

        when(institucionRepository.findById(anyLong()))
                .thenAnswer(i -> Optional.ofNullable(instituciones.get(i.getArgument(0, Long.class))));
        when(institucionRepository.findAllByInstitucionPadre_Id(anyLong()))
                .thenAnswer(i -> instituciones.values().stream()
                        .filter(x -> x.getInstitucionPadre() != null
                                && x.getInstitucionPadre().getId().equals(i.getArgument(0, Long.class)))
                        .toList());
        when(visibilidadRepository.findAllByInstitucionPadre_Id(anyLong())).thenReturn(List.of());

        arbol = new InstitucionArbolService(institucionRepository);
        visibilidad = new InstitucionVisibilidadService(
                institucionRepository, visibilidadRepository, arbol, contexto);
    }

    private void crear(Long id, Long idPadre) {
        Institucion i = new Institucion();
        i.setId(id);
        i.setNombre("Inst " + id);
        i.setVerParticipantesHijas(true);
        if (idPadre != null) i.setInstitucionPadre(instituciones.get(idPadre));
        instituciones.put(id, i);
    }

    private void ocultar(Long idPadre, Long idHija) {
        VisibilidadInstitucionHija v = new VisibilidadInstitucionHija();
        v.setInstitucionPadre(instituciones.get(idPadre));
        v.setInstitucionHija(instituciones.get(idHija));
        v.setVerParticipantes(false);
        when(visibilidadRepository.findAllByInstitucionPadre_Id(idPadre)).thenReturn(List.of(v));
        when(visibilidadRepository.findByInstitucionPadre_IdAndInstitucionHija_Id(idPadre, idHija))
                .thenReturn(Optional.of(v));
    }

    // ── El recorrido ─────────────────────────────────────────────────────────

    @Test
    void sinDecisiones_seVenTodasLasDescendientes() {
        assertEquals(Set.of(2L, 3L, 4L), visibilidad.descendientesVisibles(1L));
    }

    @Test
    void ocultarUnaHija_ocultaTambienSuRama() {
        ocultar(1L, 2L);
        // 4 cuelga de 2: verla sin ver a su madre dejaría el árbol con agujeros.
        assertEquals(Set.of(3L), visibilidad.descendientesVisibles(1L));
    }

    @Test
    void ocultarUnaHija_noAfectaALaOtra() {
        ocultar(1L, 3L);
        assertEquals(Set.of(2L, 4L), visibilidad.descendientesVisibles(1L));
    }

    @Test
    void defectoApagado_ocultaTodasLasHijas() {
        instituciones.get(1L).setVerParticipantesHijas(false);
        assertTrue(visibilidad.descendientesVisibles(1L).isEmpty());
    }

    @Test
    void defectoApagado_conExcepcionExplicita_dejaVerEsaRama() {
        instituciones.get(1L).setVerParticipantesHijas(false);
        VisibilidadInstitucionHija v = new VisibilidadInstitucionHija();
        v.setInstitucionPadre(instituciones.get(1L));
        v.setInstitucionHija(instituciones.get(2L));
        v.setVerParticipantes(true);
        when(visibilidadRepository.findAllByInstitucionPadre_Id(1L)).thenReturn(List.of(v));

        assertEquals(Set.of(2L, 4L), visibilidad.descendientesVisibles(1L));
    }

    @Test
    void laDecisionDeUnaHija_noAfectaALoQueVeElAbuelo() {
        // 2 deja de ver a 4, pero 1 sigue viendo a 2 y a 4 por su propia cuenta.
        instituciones.get(2L).setVerParticipantesHijas(false);
        assertEquals(Set.of(2L, 3L), visibilidad.descendientesVisibles(1L));
        assertTrue(visibilidad.descendientesVisibles(2L).isEmpty());
    }

    @Test
    void unCicloEnLaJerarquiaNoCuelgaElRecorrido() {
        // La base de datos no impide que alguien deje 1 colgando de 4.
        instituciones.get(1L).setInstitucionPadre(instituciones.get(4L));
        assertEquals(Set.of(2L, 3L, 4L), visibilidad.descendientesVisibles(1L));
    }

    // ── Estado para la pantalla ──────────────────────────────────────────────

    @Test
    void estadoDeHijas_marcaCuandoLaDecisionEsExplicita() {
        ocultar(1L, 2L);
        var estado = visibilidad.estadoDeHijas(1L);

        var dos = estado.stream().filter(e -> e.idHija().equals(2L)).findFirst().orElseThrow();
        var tres = estado.stream().filter(e -> e.idHija().equals(3L)).findFirst().orElseThrow();

        assertFalse(dos.verParticipantes());
        assertTrue(dos.decisionExplicita());
        assertTrue(tres.verParticipantes());
        assertFalse(tres.decisionExplicita(), "3 hereda el defecto, no tiene decisión propia");
    }

    // ── Autorización ─────────────────────────────────────────────────────────

    @Test
    void nadieDecidePorUnaInstitucionAjena() {
        when(contexto.getIdInstitucionActual()).thenReturn(3L); // hermana, no ancestra
        assertThrows(AccessDeniedException.class,
                () -> visibilidad.fijarVisibilidad(2L, 4L, false, "uuid"));
    }

    @Test
    void laPropiaInstitucionSiDecide() {
        when(contexto.getIdInstitucionActual()).thenReturn(2L);
        when(visibilidadRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> visibilidad.fijarVisibilidad(2L, 4L, false, "uuid"));
    }

    @Test
    void unaAncestraTambienDecide() {
        when(contexto.getIdInstitucionActual()).thenReturn(1L);
        when(visibilidadRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> visibilidad.fijarVisibilidad(2L, 4L, false, "uuid"));
    }

    @Test
    void soloSeDecideSobreHijasDirectas() {
        when(contexto.getIdInstitucionActual()).thenReturn(1L);
        // 4 es nieta de 1: su visibilidad la decide 2, no 1.
        assertThrows(imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException.class,
                () -> visibilidad.fijarVisibilidad(1L, 4L, false, "uuid"));
    }

    @Test
    void fijarDefecto_borraLasExcepciones() {
        when(contexto.getIdInstitucionActual()).thenReturn(1L);
        ocultar(1L, 2L);

        visibilidad.fijarDefecto(1L, false);

        assertFalse(instituciones.get(1L).getVerParticipantesHijas());
        verify(visibilidadRepository).deleteAll(any());
    }
}
