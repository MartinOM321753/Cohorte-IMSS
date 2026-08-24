package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.PermisoRegistroParticipantes;
import imss.gob.mx.cohorte.modules.institucion.PermisoRegistroParticipantesRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
// La del proyecto, no la de jakarta: el GlobalExceptionHandler solo mapea esta. Con
// la de jakarta este servicio venía devolviendo 500 en vez de explicar el motivo.
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Decide a nombre de que instituciones puede registrar participantes una sede.
 *
 * <p>Es la unica fuente de verdad del conjunto: lo consume tanto el endpoint que
 * llena el desplegable del formulario como la validacion del alta, para que la
 * lista que se ofrece y la que se acepta no puedan separarse.</p>
 *
 * <p>La regla tiene tres partes:</p>
 * <ol>
 *   <li>La propia institucion, siempre.</li>
 *   <li>Sus descendientes, siempre — coherente con la visibilidad, donde un padre
 *       ya ve todo lo de sus hijas sin permiso explicito.</li>
 *   <li>El grupo del padre (el padre y las hermanas) solo si ese padre lo
 *       autorizo mediante {@link PermisoRegistroParticipantes}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class InstitucionRegistroService {

    private final InstitucionRepository institucionRepository;
    private final PermisoRegistroParticipantesRepository permisoRepository;
    private final InstitucionArbolService arbol;
    private final InstitucionVisibilidadService visibilidad;
    private final imss.gob.mx.cohorte.security.institucion.InstitucionContextService institucionContextService;

    /**
     * Instituciones a las que {@code idInstitucionActual} puede asignar un
     * participante nuevo. El primer elemento es siempre la propia.
     */
    @Transactional(readOnly = true)
    public List<Long> getInstitucionesParaRegistro(Long idInstitucionActual) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(idInstitucionActual);

        // Descendientes que esta institución ha decidido seguir viendo. No se puede
        // dar de alta un participante en una sede que no ves: nacería invisible.
        ids.addAll(visibilidad.descendientesVisibles(idInstitucionActual));

        // El permiso lo otorga el padre y habilita todo su grupo: el propio padre
        // y las hermanas que cuelgan de el. No se otorga par por par porque la
        // autorizacion es "puede registrar dentro del grupo", no "para esta sede".
        for (PermisoRegistroParticipantes permiso :
                permisoRepository.findAllByInstitucionRecibe_IdAndHabilitadoTrue(idInstitucionActual)) {
            Institucion padre = permiso.getInstitucionOtorga();
            ids.add(padre.getId());
            for (Institucion hermana : institucionRepository.findAllByInstitucionPadre_Id(padre.getId())) {
                ids.add(hermana.getId());
            }
        }

        return new ArrayList<>(ids);
    }

    /** Version con entidades, para armar el desplegable sin una consulta por id. */
    @Transactional(readOnly = true)
    public List<Institucion> getInstitucionesParaRegistroDetalle(Long idInstitucionActual) {
        List<Long> ids = getInstitucionesParaRegistro(idInstitucionActual);
        Map<Long, Institucion> porId = new HashMap<>();
        for (Institucion i : institucionRepository.findAllById(ids)) {
            porId.put(i.getId(), i);
        }
        // Se respeta el orden de getInstitucionesParaRegistro: la propia primero.
        List<Institucion> resultado = new ArrayList<>();
        for (Long id : ids) {
            Institucion i = porId.get(id);
            if (i != null && Boolean.TRUE.equals(i.getActivo())) {
                resultado.add(i);
            }
        }
        return resultado;
    }

    /**
     * Resuelve la institucion a la que debe quedar asignado un participante nuevo.
     *
     * <p>Si no se pidio ninguna, es la del usuario. Si se pidio una, tiene que
     * estar en el conjunto autorizado: no basta con que el id exista.</p>
     */
    @Transactional(readOnly = true)
    public Institucion resolverInstitucionDestino(Long idInstitucionActual, Long idInstitucionSolicitada) {
        Long destino = (idInstitucionSolicitada != null) ? idInstitucionSolicitada : idInstitucionActual;

        if (!destino.equals(idInstitucionActual)
                && !getInstitucionesParaRegistro(idInstitucionActual).contains(destino)) {
            throw new AccessDeniedException(
                    "Tu institución no está autorizada para registrar participantes en la institución indicada");
        }

        return institucionRepository.findById(destino)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución indicada"));
    }


    // ── Administracion del permiso ──────────────────────────────────────────

    @Transactional
    public PermisoRegistroParticipantes otorgarPermiso(Long idInstitucionOtorga, Long idInstitucionRecibe) {
        verificarPuedeDisponerDelRegistro(idInstitucionOtorga);
        Institucion otorga = institucionRepository.findById(idInstitucionOtorga)
                .orElseThrow(() -> new ObjNotFoundException("Institución otorgante no encontrada"));
        Institucion recibe = institucionRepository.findById(idInstitucionRecibe)
                .orElseThrow(() -> new ObjNotFoundException("Institución receptora no encontrada"));

        if (!esAncestra(otorga, recibe)) {
            throw new ValidationException(
                    "Solo una institución padre puede autorizar el registro de participantes a una hija");
        }

        Optional<PermisoRegistroParticipantes> existente =
                permisoRepository.findByInstitucionOtorga_IdAndInstitucionRecibe_Id(idInstitucionOtorga, idInstitucionRecibe);

        if (existente.isPresent()) {
            PermisoRegistroParticipantes permiso = existente.get();
            permiso.setHabilitado(true);
            return permisoRepository.save(permiso);
        }

        PermisoRegistroParticipantes permiso = new PermisoRegistroParticipantes();
        permiso.setInstitucionOtorga(otorga);
        permiso.setInstitucionRecibe(recibe);
        permiso.setHabilitado(true);
        return permisoRepository.save(permiso);
    }

    @Transactional
    public PermisoRegistroParticipantes revocarPermiso(Long idInstitucionOtorga, Long idInstitucionRecibe) {
        verificarPuedeDisponerDelRegistro(idInstitucionOtorga);
        PermisoRegistroParticipantes permiso = permisoRepository
                .findByInstitucionOtorga_IdAndInstitucionRecibe_Id(idInstitucionOtorga, idInstitucionRecibe)
                .orElseThrow(() -> new ObjNotFoundException("Permiso no encontrado"));
        permiso.setHabilitado(false);
        return permisoRepository.save(permiso);
    }

    @Transactional(readOnly = true)
    public List<PermisoRegistroParticipantes> listarPermisosOtorgados(Long idInstitucionOtorga) {
        verificarPuedeDisponerDelRegistro(idInstitucionOtorga);
        return permisoRepository.findAllByInstitucionOtorga_Id(idInstitucionOtorga);
    }

    @Transactional(readOnly = true)
    public List<PermisoRegistroParticipantes> listarPermisosRecibidos(Long idInstitucionRecibe) {
        verificarPuedeDisponerDelRegistro(idInstitucionRecibe);
        return permisoRepository.findAllByInstitucionRecibe_IdAndHabilitadoTrue(idInstitucionRecibe);
    }

    /**
     * Copia literal del criterio que su gemelo —el permiso de acceso a pacientes—
     * ya aplicaba: sobre la autorizacion de una institucion solo deciden ella misma
     * o una superior. Aqui faltaba, y comprobar solo el parentesco entre otorgante
     * y receptora no basta: cualquier hija podia pedir que su propia raiz la
     * autorizara, que es autoconcederse el permiso.
     */
    private void verificarPuedeDisponerDelRegistro(Long idInstitucion) {
        Long idActual = institucionContextService.getIdInstitucionActual();
        if (idActual.equals(idInstitucion)) return;
        if (arbol.esAncestra(idActual, idInstitucion)) return;
        throw new org.springframework.security.access.AccessDeniedException(
                "Solo la propia institucion o una superior puede decidir sobre este permiso");
    }

    private boolean esAncestra(Institucion posibleAncestra, Institucion objetivo) {
        return arbol.esAncestra(posibleAncestra.getId(), objetivo.getId());
    }
}
