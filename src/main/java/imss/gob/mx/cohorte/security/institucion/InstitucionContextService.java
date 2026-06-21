package imss.gob.mx.cohorte.security.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Servicio central para resolver la institución del usuario autenticado.
 *
 * <p>El {@code JWTFilter} sólo coloca en el {@link SecurityContextHolder} el
 * UUID y el rol del usuario (ver {@code UDService}) — NO carga la entidad
 * completa {@link BeanUser} ni su {@link Institucion}. Por lo tanto, este
 * servicio re-consulta la base de datos para obtener la institución vigente
 * del usuario en el momento de cada solicitud (evita depender de datos
 * potencialmente obsoletos guardados en el token).</p>
 *
 * <p>Es la pieza central del aislamiento de datos por institución: los
 * ApplicationService de los módulos propios de cada institución (estudios,
 * exámenes, citas, almacenes, somatometría, etc.) deben usar
 * {@link #getInstitucionActual()} para validar/filtrar por
 * {@code institucionId}, garantizando que un usuario nunca pueda leer ni
 * mutar datos de una institución distinta a la suya.</p>
 */
@Service
@RequiredArgsConstructor
public class InstitucionContextService {

    private final UserRepository userRepository;
    private final InstitucionRepository institucionRepository;

    /**
     * Obtiene el {@link BeanUser} autenticado actual a partir del
     * {@link SecurityContextHolder}.
     *
     * @throws AccessDeniedException si no hay un usuario autenticado en el contexto
     * @throws ObjNotFoundException  si el UUID del contexto no corresponde a un usuario existente
     */
    public BeanUser getUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("No hay un usuario autenticado en el contexto de seguridad");
        }
        String uuid = auth.getName();
        return userRepository.findByUUID(uuid)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el usuario autenticado con uuid: " + uuid));
    }

    /**
     * Obtiene la institución a la que pertenece el usuario autenticado actual.
     *
     * @throws AccessDeniedException si no hay usuario autenticado o si el
     *                               usuario no tiene institución asignada
     */
    public Institucion getInstitucionActual() {
        BeanUser usuario = getUsuarioActual();
        Institucion institucion = usuario.getInstitucion();
        if (institucion == null) {
            throw new AccessDeniedException("El usuario autenticado no tiene una institución asignada");
        }
        return institucion;
    }

    /** Atajo para obtener únicamente el id de la institución del usuario actual. */
    public Long getIdInstitucionActual() {
        return getInstitucionActual().getId();
    }

    /**
     * Verifica que la institución indicada coincida con la del usuario
     * autenticado. Lanza {@link AccessDeniedException} en caso contrario —
     * usar en servicios para validar que un recurso (estudio, examen, cita,
     * almacén, somatometría, etc.) pertenece a la institución del usuario
     * antes de permitir su lectura o mutación.
     */
    public void verificarPertenece(Long idInstitucionRecurso) {
        Long idInstitucionActual = getIdInstitucionActual();
        if (idInstitucionRecurso == null || !idInstitucionActual.equals(idInstitucionRecurso)) {
            throw new AccessDeniedException("El recurso solicitado pertenece a otra institución");
        }
    }

    /**
     * Igual que {@link #verificarPertenece(Long)} pero recibiendo directamente
     * la entidad {@link Institucion} del recurso (puede ser un stub con sólo
     * el id, p. ej. proveniente de una relación lazy).
     */
    public void verificarPertenece(Institucion institucionRecurso) {
        if (institucionRecurso == null) {
            throw new AccessDeniedException("El recurso no tiene institución asociada");
        }
        verificarPertenece(institucionRecurso.getId());
    }

    /**
     * Verifica que la institución del usuario puede acceder a un recurso de
     * {@code idInstitucionRecurso}: misma institución O la del usuario es
     * ancestra (padre, abuelo, etc.) en la jerarquía.
     *
     * @throws AccessDeniedException si la institución del usuario no es la misma
     *                               ni ancestra de la institución del recurso
     */
    public void verificarPerteneceOAncestra(Long idInstitucionRecurso) {
        Long idInstUsuario = getIdInstitucionActual();
        if (idInstUsuario.equals(idInstitucionRecurso)) {
            return;
        }
        if (esAncestra(idInstUsuario, idInstitucionRecurso)) {
            return;
        }
        throw new AccessDeniedException("El recurso solicitado pertenece a otra institución");
    }

    /**
     * Recorre la cadena de padres de {@code idInstitucionHija} buscando a
     * {@code idInstitucionPosibleAncestra}.
     */
    public boolean esAncestra(Long idInstitucionPosibleAncestra, Long idInstitucionHija) {
        Institucion cursor = institucionRepository.findById(idInstitucionHija).orElse(null);
        if (cursor == null) return false;
        cursor = cursor.getInstitucionPadre();
        while (cursor != null) {
            if (cursor.getId().equals(idInstitucionPosibleAncestra)) {
                return true;
            }
            cursor = cursor.getInstitucionPadre();
        }
        return false;
    }
}
