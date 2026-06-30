package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.TipoInstitucion;
import imss.gob.mx.cohorte.modules.institucion.TipoInstitucionRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InstitucionService {

    private final InstitucionRepository institucionRepository;
    private final TipoInstitucionRepository tipoInstitucionRepository;
    private final UserRepository userRepository;
    private final InstitucionContextService institucionContextService;
    private final InstitucionJerarquiaService institucionJerarquiaService;

    /**
     * Instituciones visibles para el usuario actual según la jerarquía (propias,
     * descendientes y ancestras con permiso otorgado) — alimenta el selector de
     * institución del frontend cuando el modo "jerarquía" está activo.
     */
    @Transactional(readOnly = true)
    public List<Institucion> getVisiblesParaJerarquia() {
        Long idActual = institucionContextService.getIdInstitucionActual();
        List<Long> ids = institucionJerarquiaService.getInstitucionesVisibles(idActual);
        return institucionRepository.findAllById(ids);
    }

    @Transactional(readOnly = true)
    public Page<Institucion> getAllPaginado(Pageable pageable) {
        return institucionRepository.findAll(pageable);
    }

    /**
     * Búsqueda server-side por nombre, paginada — pensada para alimentar selects
     * con autocompletado (debounce en el cliente, filtrado real en el servidor).
     * Evita el conflicto de cargar la tabla completa para filtrarla en JS.
     */
    @Transactional(readOnly = true)
    public Page<Institucion> search(String texto, boolean soloActivas, Pageable pageable) {
        String q = texto == null ? "" : texto.trim();
        return soloActivas
                ? institucionRepository.findAllByActivoTrueAndNombreContainingIgnoreCase(q, pageable)
                : institucionRepository.findAllByNombreContainingIgnoreCase(q, pageable);
    }

    @Transactional(readOnly = true)
    public List<Institucion> getAllActivas() {
        return institucionRepository.findAllByActivo(true);
    }

    @Transactional(readOnly = true)
    public Institucion getById(Long id) {
        return institucionRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución con id: " + id));
    }

    @Transactional(readOnly = true)
    public Institucion getByUuid(String uuid) {
        return institucionRepository.findByUuid(uuid)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución con UUID: " + uuid));
    }

    @Transactional(readOnly = true)
    public List<Institucion> getRaices() {
        return institucionRepository.findAllByInstitucionPadreIsNull();
    }

    @Transactional(readOnly = true)
    public List<Institucion> getHijas(Long idPadre) {
        return institucionRepository.findAllByInstitucionPadre_Id(idPadre);
    }

    @Transactional
    public Institucion create(Institucion institucion, Long idTipoInstitucion, Long idInstitucionPadre, String uuidEncargado) {
        String nombre = institucion.getNombre().trim();
        if (institucionRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ObjConflictException("Ya existe una institución con el nombre: " + nombre);
        }
        institucion.setNombre(nombre);

        institucion.setTipoInstitucion(resolverTipo(idTipoInstitucion));
        institucion.setInstitucionPadre(resolverPadre(idInstitucionPadre, null));

        BeanUser encargadoParaMigrar = null;
        if (uuidEncargado != null && !uuidEncargado.isBlank()) {
            BeanUser encargado = resolverEncargado(uuidEncargado);
            institucion.setEncargado(encargado);
            encargadoParaMigrar = encargado;
        }

        institucion.setUuid(UUID.randomUUID().toString());
        institucion.setActivo(true);
        institucion.setFechaRegistro(Timestamp.from(Instant.now()));
        Institucion saved = institucionRepository.save(institucion);

        // Migrar la institución del encargado: al ser asignado como encargado de esta
        // institución nueva, su contexto de datos cambia a ella automáticamente.
        if (encargadoParaMigrar != null) {
            encargadoParaMigrar.setInstitucion(saved);
            userRepository.save(encargadoParaMigrar);
        }

        return saved;
    }

    @Transactional
    public Institucion update(Long id, Institucion institucion, Long idTipoInstitucion, Long idInstitucionPadre, String uuidEncargado) {
        Institucion bd = getById(id);
        verificarAutoridad(bd, institucionContextService.getUsuarioActual());

        String nombre = institucion.getNombre().trim();
        if (!bd.getNombre().equalsIgnoreCase(nombre) && institucionRepository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ObjConflictException("Ya existe otra institución con el nombre: " + nombre);
        }

        bd.setNombre(nombre);
        bd.setLatitud(institucion.getLatitud());
        bd.setLongitud(institucion.getLongitud());
        bd.setEstado(institucion.getEstado());
        bd.setCiudad(institucion.getCiudad());
        bd.setDireccion(institucion.getDireccion());
        bd.setResponsable(institucion.getResponsable());
        bd.setTelefono(institucion.getTelefono());
        if (institucion.getTieneBiobanco() != null) bd.setTieneBiobanco(institucion.getTieneBiobanco());
        if (institucion.getActivo() != null) bd.setActivo(institucion.getActivo());

        if (idTipoInstitucion != null) {
            bd.setTipoInstitucion(resolverTipo(idTipoInstitucion));
        }

        bd.setInstitucionPadre(resolverPadre(idInstitucionPadre, bd));

        BeanUser encargadoParaMigrar = null;
        if (uuidEncargado != null && !uuidEncargado.isBlank()) {
            BeanUser encargado = resolverEncargado(uuidEncargado);
            bd.setEncargado(encargado);
            encargadoParaMigrar = encargado;
        } else if (uuidEncargado != null) {
            bd.setEncargado(null);
        }

        Institucion updated = institucionRepository.save(bd);

        // Migrar la institución del encargado al actualizar su asignación.
        if (encargadoParaMigrar != null) {
            encargadoParaMigrar.setInstitucion(updated);
            userRepository.save(encargadoParaMigrar);
        }

        return updated;
    }

    @Transactional
    public Institucion toggleActivo(Long id) {
        Institucion bd = getById(id);
        BeanUser usuarioActual = institucionContextService.getUsuarioActual();
        verificarAutoridadParaCambiarEstado(bd, usuarioActual);

        boolean nuevoEstado = !Boolean.TRUE.equals(bd.getActivo());
        bd.setActivo(nuevoEstado);
        if (!nuevoEstado) {
            userRepository.findAllByInstitucion_Id(bd.getId()).forEach(usuario -> {
                usuario.setActivo(false);
                userRepository.save(usuario);
            });
        }
        return institucionRepository.save(bd);
    }

    /**
     * Retorna los IDs de todas las instituciones que el usuario actual puede gestionar:
     * aquellas donde es encargado + todos sus descendientes + las que no tienen encargado (bootstrap).
     */
    @Transactional(readOnly = true)
    public Set<Long> getIdsGestionables() {
        BeanUser usuario = institucionContextService.getUsuarioActual();
        Set<Long> resultado = new HashSet<>();

        // Instituciones sin encargado: cualquier ADMINISTRADOR puede configurarlas
        institucionRepository.findAllByEncargadoIsNull().forEach(i -> resultado.add(i.getId()));

        // Instituciones donde el usuario es encargado + todos sus descendientes
        List<Institucion> encargadoDe = institucionRepository.findAllByEncargado_UUID(usuario.getUUID());
        for (Institucion raiz : encargadoDe) {
            recolectarDescendientes(raiz, resultado);
        }

        return resultado;
    }

    @Transactional(readOnly = true)
    public Set<Long> getIdsConEstadoGestionable() {
        BeanUser usuario = institucionContextService.getUsuarioActual();
        Set<Long> resultado = new HashSet<>();

        List<Institucion> encargadoDe = institucionRepository.findAllByEncargado_UUID(usuario.getUUID());
        for (Institucion institucion : encargadoDe) {
            if (institucion.getInstitucionPadre() == null) {
                resultado.add(institucion.getId());
            }
            recolectarDescendientesSinRaiz(institucion, resultado);
        }

        return resultado;
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private TipoInstitucion resolverTipo(Long idTipo) {
        if (idTipo == null) {
            throw new ValidationException("El tipo de institución es obligatorio.");
        }
        return tipoInstitucionRepository.findById(idTipo)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tipo de institución con id: " + idTipo));
    }

    /** Valida que la institución no se asigne a sí misma, ni genere ciclos en el árbol. */
    private Institucion resolverPadre(Long idPadre, Institucion actual) {
        if (idPadre == null) return null;

        if (actual != null && idPadre.equals(actual.getId())) {
            throw new ValidationException("Una institución no puede ser su propia institución padre.");
        }

        Institucion padre = institucionRepository.findById(idPadre)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución padre con id: " + idPadre));

        if (actual != null) {
            Institucion cursor = padre;
            while (cursor != null) {
                if (cursor.getId().equals(actual.getId())) {
                    throw new ValidationException("La jerarquía de instituciones no puede formar ciclos: "
                            + "la institución seleccionada como padre es descendiente de esta institución.");
                }
                cursor = cursor.getInstitucionPadre();
            }
        }

        return padre;
    }

    /**
     * Verifica que el usuario tenga autoridad sobre la institución destino:
     * es el encargado de la institución, o el encargado de alguna institución ancestral.
     * Si la institución no tiene encargado se permite el acceso (bootstrap).
     */
    private void verificarAutoridad(Institucion destino, BeanUser usuario) {
        if (destino.getEncargado() == null) {
            return; // Bootstrap: sin encargado, cualquier ADMINISTRADOR puede configurarla
        }
        if (destino.getEncargado().getId().equals(usuario.getId())) {
            return; // Es el encargado directo
        }
        // Recorrer la cadena de ancestros buscando si el usuario es encargado de alguno
        Institucion cursor = destino.getInstitucionPadre();
        while (cursor != null) {
            if (cursor.getEncargado() != null && cursor.getEncargado().getId().equals(usuario.getId())) {
                return;
            }
            cursor = cursor.getInstitucionPadre();
        }
        throw new AccessDeniedException(
                "No tienes autoridad para gestionar la institución '" + destino.getNombre()
                + "'. Solo su encargado o el encargado de una institución superior pueden hacerlo.");
    }

    /** Agrega la institución y todos sus descendientes al conjunto de IDs. */
    private void verificarAutoridadParaCambiarEstado(Institucion destino, BeanUser usuario) {
        if (destino.getEncargado() == null) {
            throw new AccessDeniedException("No se puede cambiar el estado de una institucion sin encargado asignado.");
        }

        if (destino.getInstitucionPadre() == null) {
            if (destino.getEncargado().getId().equals(usuario.getId())) {
                return;
            }
            throw new AccessDeniedException("Solo el encargado de la institucion raiz puede cambiar su estado.");
        }

        Institucion cursor = destino.getInstitucionPadre();
        while (cursor != null) {
            if (cursor.getEncargado() != null && cursor.getEncargado().getId().equals(usuario.getId())) {
                return;
            }
            cursor = cursor.getInstitucionPadre();
        }

        throw new AccessDeniedException(
                "Solo el encargado de una institucion superior puede cambiar el estado de '"
                + destino.getNombre() + "'.");
    }

    private void recolectarDescendientes(Institucion institucion, Set<Long> ids) {
        ids.add(institucion.getId());
        List<Institucion> hijas = institucionRepository.findAllByInstitucionPadre_Id(institucion.getId());
        for (Institucion hija : hijas) {
            recolectarDescendientes(hija, ids);
        }
    }

    private void recolectarDescendientesSinRaiz(Institucion institucion, Set<Long> ids) {
        List<Institucion> hijas = institucionRepository.findAllByInstitucionPadre_Id(institucion.getId());
        for (Institucion hija : hijas) {
            recolectarDescendientes(hija, ids);
        }
    }

    private BeanUser resolverEncargado(String uuid) {
        BeanUser usuario = userRepository.findByUUID(uuid)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el usuario encargado con UUID: " + uuid));

        if (!usuario.getActivo()) {
            throw new ValidationException("El usuario seleccionado como encargado está inactivo.");
        }

        String rol = usuario.getRol().getRole();
        if (!"ENCARGADO".equals(rol) && !"ADMINISTRADOR".equals(rol)) {
            throw new ValidationException(
                    "El usuario '" + usuario.getUsername() + "' no tiene un rol válido para ser encargado de institución (tiene: " + rol + ").");
        }

        return usuario;
    }
}
