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
 * Quién puede abrir el padrón de quién, y qué entra en cada conjunto.
 *
 * <p>Árbol: 1 → {2, 3}. 2 y 3 son hermanas.</p>
 */
class AccesoPadronTest {

    private InstitucionRepository institucionRepository;
    private PermisoAccesoPacientesRepository permisoRepository;
    private InstitucionRegistroService registro;
    private InstitucionContextService contexto;
    private InstitucionArbolService arbol;
    private InstitucionJerarquiaService jerarquia;

    private final Map<Long, Institucion> instituciones = new HashMap<>();

    @BeforeEach
    void setUp() {
        institucionRepository = mock(InstitucionRepository.class);
        permisoRepository = mock(PermisoAccesoPacientesRepository.class);
        registro = mock(InstitucionRegistroService.class);
        contexto = mock(InstitucionContextService.class);

        crear(1L, null);
        crear(2L, 1L);
        crear(3L, 1L);

        when(institucionRepository.findById(anyLong()))
                .thenAnswer(i -> Optional.ofNullable(instituciones.get(i.getArgument(0, Long.class))));
        when(institucionRepository.findAllByInstitucionPadre_Id(anyLong()))
                .thenAnswer(i -> instituciones.values().stream()
                        .filter(x -> x.getInstitucionPadre() != null
                                && x.getInstitucionPadre().getId().equals(i.getArgument(0, Long.class)))
                        .toList());
        when(permisoRepository.findAllByInstitucionRecibe_IdAndHabilitadoTrue(anyLong())).thenReturn(List.of());
        when(permisoRepository.findByInstitucionOtorga_IdAndInstitucionRecibe_Id(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(permisoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(registro.getInstitucionesParaRegistro(anyLong()))
                .thenAnswer(i -> List.of(i.getArgument(0, Long.class)));

        arbol = new InstitucionArbolService(institucionRepository);
        jerarquia = new InstitucionJerarquiaService(
                institucionRepository, permisoRepository, registro, arbol, contexto);
    }

    private void crear(Long id, Long idPadre) {
        Institucion i = new Institucion();
        i.setId(id);
        i.setNombre("Inst " + id);
        i.setActivo(true);
        if (idPadre != null) i.setInstitucionPadre(instituciones.get(idPadre));
        instituciones.put(id, i);
    }

    // ── Quién puede otorgar ──────────────────────────────────────────────────

    @Test
    void unaHermanaPuedeAbrirSuPadronALaOtra() {
        when(contexto.getIdInstitucionActual()).thenReturn(2L);
        assertDoesNotThrow(() -> jerarquia.otorgarPermiso(2L, 3L));
    }

    @Test
    void nadiePuedeOtorgarANombreDeUnaInstitucionAjena() {
        // 3 intenta regalar el padrón de su hermana 2 cambiando el id de la URL.
        when(contexto.getIdInstitucionActual()).thenReturn(3L);
        assertThrows(AccessDeniedException.class, () -> jerarquia.otorgarPermiso(2L, 3L));
    }

    @Test
    void unaAncestraSiPuedeOtorgarPorSuDescendiente() {
        when(contexto.getIdInstitucionActual()).thenReturn(1L);
        assertDoesNotThrow(() -> jerarquia.otorgarPermiso(2L, 3L));
    }

    @Test
    void revocarExigeLaMismaAutorizacionQueOtorgar() {
        when(contexto.getIdInstitucionActual()).thenReturn(3L);
        assertThrows(AccessDeniedException.class, () -> jerarquia.revocarPermiso(2L, 3L));
    }

    @Test
    void noSeOtorgaASiMisma() {
        when(contexto.getIdInstitucionActual()).thenReturn(2L);
        assertThrows(imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException.class,
                () -> jerarquia.otorgarPermiso(2L, 2L));
    }

    @Test
    void noSeOtorgaAUnaInstitucionInactiva() {
        instituciones.get(3L).setActivo(false);
        when(contexto.getIdInstitucionActual()).thenReturn(2L);
        assertThrows(imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException.class,
                () -> jerarquia.otorgarPermiso(2L, 3L));
    }

    // ── Qué entra en cada conjunto ───────────────────────────────────────────

    @Test
    void elPermisoNoEsTransitivo() {
        // 1 abre a 2, y 2 abre a 3. 3 ve a 2, pero no a 1.
        permisoVivo(1L, 2L);
        permisoVivo(2L, 3L);

        assertEquals(List.of(3L, 2L), jerarquia.getInstitucionesVisibles(3L));
        assertFalse(jerarquia.getInstitucionesVisibles(3L).contains(1L),
                "encadenar dos acuerdos bilaterales crearía una red que nadie autorizó");
    }

    @Test
    void elSelectorConservaLasHijasQueNoSeVen() {
        // El alcance de participantes de 1 es solo ella (visibilidad apagada), pero
        // el selector tiene que seguir ofreciendo a 2 y 3 para poder administrarlas
        // y volver a mostrarlas.
        when(registro.getInstitucionesParaRegistro(1L)).thenReturn(List.of(1L));

        assertEquals(List.of(1L), jerarquia.getInstitucionesVisibles(1L));
        assertTrue(jerarquia.getInstitucionesParaSelector(1L).containsAll(List.of(1L, 2L, 3L)));
    }

    private void permisoVivo(Long otorga, Long recibe) {
        PermisoAccesoPacientes p = new PermisoAccesoPacientes();
        p.setInstitucionOtorga(instituciones.get(otorga));
        p.setInstitucionRecibe(instituciones.get(recibe));
        p.setHabilitado(true);
        List<PermisoAccesoPacientes> previos =
                new ArrayList<>(permisoRepository.findAllByInstitucionRecibe_IdAndHabilitadoTrue(recibe));
        previos.add(p);
        when(permisoRepository.findAllByInstitucionRecibe_IdAndHabilitadoTrue(recibe)).thenReturn(previos);
    }
}
