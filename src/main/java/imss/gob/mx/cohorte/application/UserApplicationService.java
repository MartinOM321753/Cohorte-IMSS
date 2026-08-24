package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.permisos.UsuarioRol;
import imss.gob.mx.cohorte.modules.permisos.UsuarioRolRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.auth.PasswordResetService;
import imss.gob.mx.cohorte.services.permisos.PermisoEvaluationService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserApplicationService {

    private final UserService userService;
    private final PersonaService personaService;
    private final RoleRepository roleRepository;
    private final InstitucionRepository institucionRepository;
    private final PasswordResetService passwordResetService;
    private final InstitucionContextService institucionContextService;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PermisoEvaluationService permisoEvaluationService;

    @Transactional(readOnly = true)
    public List<BeanUser> findAllByInstitucion() {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        return userService.getAllByInstitucionConInvitacionesPendientes(idInstitucion);
    }

    @Transactional(readOnly = true)
    public List<BeanUser> findAllActiveByInstitucion() {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        return userService.getAllActiveByInstitucion(idInstitucion);
    }

    @Transactional(readOnly = true)
    public Page<BeanUser> buscarPaginado(String buscar, Pageable pageable) {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        return userService.buscarPaginadoConInvitacionesPendientes(idInstitucion, buscar, pageable);
    }

    @Transactional
    public List<BeanUser> findAllUser() {
        return userService.getAllUser();
    }

    @Transactional
    public List<BeanUser> findAllByActive() {
        return userService.getAllUserByStatus(true);
    }

    @Transactional
    public List<BeanUser> findAllByInActive() {
        return userService.getAllUserByStatus(false);
    }

    @Transactional
    public BeanUser findUser(Long id) {
        BeanUser user = userService.getUser(id);
        verificarAlcanceSobreUsuario(user);
        return user;
    }

    @Transactional
    public BeanUser findByUUID(String uuid) {
        BeanUser user = userService.getByUUID(uuid);
        verificarAlcanceSobreUsuario(user);
        return user;
    }

    @Transactional(readOnly = true)
    public List<BeanUser> findByRoleName(String roleName) {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        return userService.getUsersByRoleAndInstitucion(roleName, idInstitucion);
    }

    @Transactional(readOnly = true)
    public List<BeanUser> getAdministradoresDisponibles() {
        return userService.getAdministradoresDisponibles();
    }

    @Transactional(readOnly = true)
    public List<BeanUser> getAdministradoresDisponiblesParaInstitucion(String uuidInstitucion) {
        return userService.getAdministradoresDisponiblesParaInstitucion(uuidInstitucion);
    }

    private static final String ROOT_ROLE = "ROOT";

    private boolean isCallerRoot() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ROOT"::equals);
    }

    @SuppressWarnings("deprecation")
    @Transactional
    public BeanUser saveUser(BeanUser beanUser) {
        Persona persona = beanUser.getPersona();
        Persona savePersona = personaService.createPerson(persona);
        Role findRole = roleRepository.findByUuid(beanUser.getRol().getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el rol solicitado"));

        if (ROOT_ROLE.equals(findRole.getRole()) && !isCallerRoot()) {
            throw new ValidationException("No tiene permisos para asignar este rol");
        }
        Institucion institucion = resolverInstitucion(beanUser.getInstitucion());

        String username = generarUsername(persona.getNombre(), persona.getApellidoPaterno());
        String rawPassword = generarPasswordSeguro();

        beanUser.setUsername(username);
        beanUser.setRol(findRole);
        beanUser.setInstitucion(institucion);
        beanUser.setPersona(savePersona);
        beanUser.setPassword(rawPassword);
        beanUser.setDebeResetear(true);

        BeanUser saved = userService.save(beanUser);

        UsuarioRol ur = new UsuarioRol();
        ur.setUsuario(saved);
        ur.setRol(findRole);
        ur.setFechaAsignacion(LocalDateTime.now());
        usuarioRolRepository.save(ur);

        passwordResetService.enviarInvitacion(saved);

        return saved;
    }

    @SuppressWarnings("deprecation")
    @Transactional
    public BeanUser updateUser(BeanUser beanUser) {
        BeanUser existing = userService.getUser(beanUser.getId());
        verificarAlcanceSobreUsuario(existing);

        boolean targetIsRoot = existing.getRol() != null && ROOT_ROLE.equals(existing.getRol().getRole());
        if (targetIsRoot && !isCallerRoot()) {
            throw new ValidationException("No tiene permisos para modificar este usuario");
        }
        if (targetIsRoot && isCallerRoot()) {
            BeanUser actual = institucionContextService.getUsuarioActual();
            if (!existing.getUUID().equals(actual.getUUID())) {
                throw new ValidationException("Un usuario ROOT no puede editar a otro usuario ROOT.");
            }
        }

        beanUser.getPersona().setId(existing.getPersona().getId());
        Persona updatePersona = personaService.update(beanUser.getPersona());
        Role updatedRole = roleRepository.findByUuid(beanUser.getRol().getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el rol solicitado"));

        if (ROOT_ROLE.equals(updatedRole.getRole()) && !isCallerRoot()) {
            throw new ValidationException("No tiene permisos para asignar este rol");
        }

        Institucion updatedInstitucion = resolverInstitucion(beanUser.getInstitucion());

        beanUser.setPersona(updatePersona);
        beanUser.setRol(updatedRole);
        beanUser.setInstitucion(updatedInstitucion);
        beanUser.setActivo(existing.getActivo());
        BeanUser saved = userService.updateUser(beanUser);

        usuarioRolRepository.deleteAllByUsuario(saved);
        usuarioRolRepository.flush();
        UsuarioRol ur = new UsuarioRol();
        ur.setUsuario(saved);
        ur.setRol(updatedRole);
        ur.setFechaAsignacion(LocalDateTime.now());
        usuarioRolRepository.save(ur);

        return saved;
    }

    @SuppressWarnings("deprecation")
    @Transactional
    public BeanUser toggleActivo(Long id) {
        BeanUser user = userService.getUser(id);

        boolean targetIsRoot = user.getRol() != null && ROOT_ROLE.equals(user.getRol().getRole());
        if (targetIsRoot && !isCallerRoot()) {
            throw new ValidationException("No tiene permisos para modificar este usuario");
        }

        boolean nuevoEstado = !Boolean.TRUE.equals(user.getActivo());
        verificarPuedeCambiarEstadoUsuario(user, nuevoEstado);
        return userService.setActivo(id, nuevoEstado);
    }

    @Transactional
    public BeanUser reenviarInvitacion(String uuid) {
        BeanUser user = userService.getByUUID(uuid);
        if (!Boolean.TRUE.equals(user.getActivo())) {
            throw new ValidationException("No se puede reenviar invitacion a un usuario inactivo");
        }
        if (!Boolean.TRUE.equals(user.getDebeResetear())) {
            throw new ValidationException("El usuario ya definio su contrasena inicial");
        }
        verificarPuedeReenviarInvitacion(user);

        passwordResetService.enviarInvitacion(user);
        return user;
    }

    @Transactional
    public BeanUser onlySaveUser(BeanUser beanUser) {
        return userService.save(beanUser);
    }

    @Transactional
    public BeanUser onlyUpdateUser(BeanUser beanUser) {
        return userService.updateUser(beanUser);
    }

    private Institucion resolverInstitucion(Institucion referencia) {
        if (referencia == null || referencia.getUuid() == null || referencia.getUuid().isBlank()) {
            throw new ObjNotFoundException("La institucion es obligatoria para crear o actualizar un usuario");
        }
        Institucion resuelta = institucionRepository.findByUuid(referencia.getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la institucion solicitada"));
        // Sin esto, el alta y la edicion aceptan el uuid de cualquier institucion:
        // se podria crear un usuario dentro de una institucion ajena, o sacar a uno
        // de la propia mandandolo a otra.
        verificarAlcanceSobreInstitucion(resuelta.getId());
        return resuelta;
    }

    private void verificarPuedeReenviarInvitacion(BeanUser user) {
        try {
            institucionContextService.verificarPerteneceOAncestra(user.getInstitucion().getId());
            return;
        } catch (AccessDeniedException ignored) {
            // Usuarios con invitacion pendiente pueden haber sido migrados a la institucion
            // que administraran antes de definir su contrasena. Permitir recuperar ese acceso.
        }

        BeanUser actual = institucionContextService.getUsuarioActual();
        if (!permisoEvaluationService.tienePermiso(actual, "USUARIOS_CREAR")) {
            throw new AccessDeniedException("No tienes permiso para reenviar esta invitacion");
        }
    }

    private void verificarPuedeCambiarEstadoUsuario(BeanUser objetivo, boolean nuevoEstado) {
        BeanUser actual = institucionContextService.getUsuarioActual();

        if (!nuevoEstado && objetivo.getUUID() != null && objetivo.getUUID().equals(actual.getUUID())) {
            throw new ValidationException("No puedes desactivar tu propia cuenta.");
        }

        verificarAlcanceSobreUsuario(objetivo, actual);

        if (!nuevoEstado) {
            List<Institucion> institucionesEncargadas =
                    institucionRepository.findAllByEncargado_Id(objetivo.getId());
            for (Institucion institucion : institucionesEncargadas) {
                verificarPuedeDesactivarEncargadoDeInstitucion(institucion, actual);
            }
        }
    }

    /**
     * Alcance de administracion sobre una cuenta: la propia institucion y las que
     * cuelgan de ella. Tener USUARIOS_EDITAR dice que sabes administrar usuarios,
     * no de quien: sin esta comprobacion basta cambiar el id de la peticion para
     * leer o editar la cuenta de alguien de otra institucion.
     */
    private void verificarAlcanceSobreUsuario(BeanUser objetivo) {
        verificarAlcanceSobreUsuario(objetivo, institucionContextService.getUsuarioActual());
    }

    private void verificarAlcanceSobreUsuario(BeanUser objetivo, BeanUser actual) {
        Long idInstitucionObjetivo = objetivo.getInstitucion() != null ? objetivo.getInstitucion().getId() : null;
        Long idInstitucionActual = actual.getInstitucion() != null ? actual.getInstitucion().getId() : null;
        if (idInstitucionObjetivo == null || idInstitucionActual == null) {
            throw new AccessDeniedException("No se pudo validar la institucion del usuario.");
        }
        if (idInstitucionActual.equals(idInstitucionObjetivo)
                || institucionContextService.esAncestra(idInstitucionActual, idInstitucionObjetivo)) {
            return;
        }
        throw new AccessDeniedException("No tienes permiso para administrar a este usuario.");
    }

    /** El mismo alcance, cuando lo que se valida es la institucion y no la cuenta. */
    private void verificarAlcanceSobreInstitucion(Long idInstitucionObjetivo) {
        BeanUser actual = institucionContextService.getUsuarioActual();
        Long idInstitucionActual = actual.getInstitucion() != null ? actual.getInstitucion().getId() : null;
        if (idInstitucionObjetivo == null || idInstitucionActual == null) {
            throw new AccessDeniedException("No se pudo validar la institucion del usuario.");
        }
        if (idInstitucionActual.equals(idInstitucionObjetivo)
                || institucionContextService.esAncestra(idInstitucionActual, idInstitucionObjetivo)) {
            return;
        }
        throw new AccessDeniedException("No tienes permiso para administrar usuarios de esa institucion.");
    }

    private void verificarPuedeDesactivarEncargadoDeInstitucion(Institucion institucion, BeanUser actual) {
        if (institucion.getInstitucionPadre() == null) {
            throw new AccessDeniedException(
                    "El encargado de una institucion raiz no puede desactivarse desde el modulo de usuarios.");
        }

        Institucion cursor = institucion.getInstitucionPadre();
        while (cursor != null) {
            if (cursor.getEncargado() != null && cursor.getEncargado().getId().equals(actual.getId())) {
                return;
            }
            cursor = cursor.getInstitucionPadre();
        }

        throw new AccessDeniedException(
                "Solo el encargado de una institucion superior puede desactivar al encargado de '"
                + institucion.getNombre() + "'.");
    }

    private String generarUsername(String nombre, String apellidoPaterno) {
        String baseNombre = normalizarSegmento(nombre);
        String baseApellido = normalizarSegmento(apellidoPaterno);
        String base = (baseNombre + baseApellido).trim();
        if (base.isBlank()) base = "usuario";

        SecureRandom rng = new SecureRandom();
        String candidato;
        do {
            candidato = base + (100 + rng.nextInt(900)); // 3 digitos: 100-999
        } while (userService.findByUsername(candidato).isPresent());
        return candidato;
    }

    private String normalizarSegmento(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String primerToken = texto.trim().split("\\s+")[0];
        return Normalizer.normalize(primerToken, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private String generarPasswordSeguro() {
        return imss.gob.mx.cohorte.utils.CredentialGenerator.generarPasswordSeguro();
    }
}
