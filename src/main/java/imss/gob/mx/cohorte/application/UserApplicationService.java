package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.auth.PasswordResetService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
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
        return userService.getUser(id);
    }

    @Transactional
    public BeanUser findByUUID(String uuid) {
        return userService.getByUUID(uuid);
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

    @Transactional
    public BeanUser saveUser(BeanUser beanUser) {
        Persona persona = beanUser.getPersona();
        Persona savePersona = personaService.createPerson(persona);
        Role findRole = roleRepository.findByUuid(beanUser.getRol().getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el rol solicitado"));
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
        passwordResetService.enviarInvitacion(saved);

        return saved;
    }

    @Transactional
    public BeanUser updateUser(BeanUser beanUser) {
        BeanUser existing = userService.getUser(beanUser.getId());
        beanUser.getPersona().setId(existing.getPersona().getId());
        Persona updatePersona = personaService.update(beanUser.getPersona());
        Role updatedRole = roleRepository.findByUuid(beanUser.getRol().getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el rol solicitado"));
        Institucion updatedInstitucion = resolverInstitucion(beanUser.getInstitucion());

        beanUser.setPersona(updatePersona);
        beanUser.setRol(updatedRole);
        beanUser.setInstitucion(updatedInstitucion);
        beanUser.setActivo(existing.getActivo());
        return userService.updateUser(beanUser);
    }

    @Transactional
    public BeanUser toggleActivo(Long id) {
        BeanUser user = userService.getUser(id);
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
        return institucionRepository.findByUuid(referencia.getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la institucion solicitada"));
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
        String rolActual = actual.getRol() != null ? actual.getRol().getRole() : "";
        if (!"ADMINISTRADOR".equals(rolActual)) {
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
        throw new AccessDeniedException("No tienes permiso para cambiar el estado de este usuario.");
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

        if (userService.findByUsername(base).isEmpty()) return base;

        int suffix = 2;
        while (userService.findByUsername(base + suffix).isPresent()) suffix++;
        return base + suffix;
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
        final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String lower = "abcdefghijklmnopqrstuvwxyz";
        final String digits = "0123456789";
        final String special = "!@#$%&*";
        final String all = upper + lower + digits + special;

        SecureRandom rng = new SecureRandom();
        char[] pw = new char[16];

        pw[0] = upper.charAt(rng.nextInt(upper.length()));
        pw[1] = lower.charAt(rng.nextInt(lower.length()));
        pw[2] = digits.charAt(rng.nextInt(digits.length()));
        pw[3] = special.charAt(rng.nextInt(special.length()));

        for (int i = 4; i < pw.length; i++) {
            pw[i] = all.charAt(rng.nextInt(all.length()));
        }

        for (int i = pw.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = pw[i];
            pw[i] = pw[j];
            pw[j] = tmp;
        }

        return new String(pw);
    }
}
