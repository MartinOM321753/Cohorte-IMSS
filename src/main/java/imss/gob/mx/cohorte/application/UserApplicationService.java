package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

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
    private final EmailService emailService;
    private final InstitucionContextService institucionContextService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ── Consultas ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BeanUser> findAllByInstitucion() {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        return userService.getAllByInstitucion(idInstitucion);
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

    /** Administradores activos sin encargado asignado — para selector al crear institución. */
    @Transactional(readOnly = true)
    public List<BeanUser> getAdministradoresDisponibles() {
        return userService.getAdministradoresDisponibles();
    }

    /** Administradores disponibles para una institución específica (incluye el ya asignado). */
    @Transactional(readOnly = true)
    public List<BeanUser> getAdministradoresDisponiblesParaInstitucion(String uuidInstitucion) {
        return userService.getAdministradoresDisponiblesParaInstitucion(uuidInstitucion);
    }

    // ── Creación ───────────────────────────────────────────────────────────────

    /**
     * Crea un usuario con contraseña generada por el sistema y envía las credenciales
     * al correo electrónico registrado. El usuario deberá cambiar su contraseña
     * en el primer inicio de sesión (debeResetear = true).
     */
    @Transactional
    public BeanUser saveUser(BeanUser beanUser) {
        Persona persona = beanUser.getPersona();
        Persona savePersona = personaService.createPerson(persona);
        Role findRole = roleRepository.findByUuid(beanUser.getRol().getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el rol solicitado"));
        Institucion institucion = resolverInstitucion(beanUser.getInstitucion());

        // Generar username a partir de nombre + apellidoPaterno (sin acentos, sin espacios)
        String username = generarUsername(persona.getNombre(), persona.getApellidoPaterno());

        // Generar contraseña segura aleatoria
        String rawPassword = generarPasswordSeguro();

        beanUser.setUsername(username);
        beanUser.setRol(findRole);
        beanUser.setInstitucion(institucion);
        beanUser.setPersona(savePersona);
        beanUser.setPassword(rawPassword);   // UserService.save() la encriptará
        beanUser.setDebeResetear(true);       // Forzar cambio en primer login

        BeanUser saved = userService.save(beanUser);

        // Enviar correo de bienvenida con credenciales (best-effort: no falla la creación)
        enviarEmailBienvenida(saved, rawPassword);

        return saved;
    }

    // ── Actualización ──────────────────────────────────────────────────────────

    @Transactional
    public BeanUser updateUser(BeanUser beanUser) {
        BeanUser existing = userService.getUser(beanUser.getId());
        beanUser.getPersona().setId(existing.getPersona().getId());
        Persona updatePersona = personaService.update(beanUser.getPersona());
        Role updatedRole = roleRepository.findByUuid(beanUser.getRol().getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el rol solicitado"));
        Institucion updatedInstitucion = resolverInstitucion(beanUser.getInstitucion());

        beanUser.setPersona(updatePersona);
        beanUser.setRol(updatedRole);
        beanUser.setInstitucion(updatedInstitucion);
        beanUser.setActivo(existing.getActivo());
        return userService.updateUser(beanUser);
    }

    // ── Activar / desactivar ───────────────────────────────────────────────────

    /**
     * Invierte el estado {@code activo} del usuario. Retorna el usuario ya actualizado.
     */
    @Transactional
    public BeanUser toggleActivo(Long id) {
        BeanUser user = userService.getUser(id);
        boolean nuevoEstado = !Boolean.TRUE.equals(user.getActivo());
        return userService.setActivo(id, nuevoEstado);
    }

    @Transactional
    public BeanUser onlySaveUser(BeanUser beanUser) {
        return userService.save(beanUser);
    }

    @Transactional
    public BeanUser onlyUpdateUser(BeanUser beanUser) {
        return userService.updateUser(beanUser);
    }

    // ── Helpers privados ───────────────────────────────────────────────────────

    /**
     * Resuelve la institución (recibida sólo con UUID desde el DTO/mapper) a la
     * entidad gestionada vigente en BD. Lanza {@link ObjNotFoundException} si no
     * existe — la institución es obligatoria para todo usuario del sistema.
     */
    private Institucion resolverInstitucion(Institucion referencia) {
        if (referencia == null || referencia.getUuid() == null || referencia.getUuid().isBlank()) {
            throw new ObjNotFoundException("La institución es obligatoria para crear o actualizar un usuario");
        }
        return institucionRepository.findByUuid(referencia.getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución solicitada"));
    }

    private void enviarEmailBienvenida(BeanUser user, String rawPassword) {
        String email = user.getPersona().getEmail();
        if (email == null || email.isBlank()) {
            log.warn("No se puede enviar email de bienvenida: usuario '{}' sin correo", user.getUsername());
            return;
        }
        try {
            String nombre = user.getPersona().getNombre() + " " + user.getPersona().getApellidoPaterno();
            Context ctx = new Context();
            ctx.setVariable("nombre",    nombre);
            ctx.setVariable("username",  user.getUsername());
            ctx.setVariable("password",  rawPassword);
            ctx.setVariable("loginUrl",  frontendUrl + "/login");

            emailService.enviar(
                    email,
                    "Bienvenido al Sistema Cohorte — Tus credenciales de acceso",
                    "email/bienvenida-usuario",
                    ctx
            );
        } catch (Exception e) {
            // El correo no es crítico: el usuario puede resetear su contraseña después
            log.error("Error enviando email de bienvenida a '{}': {}", email, e.getMessage());
        }
    }

    /**
     * Genera un nombre de usuario único a partir del primer nombre y primer apellido.
     * Ejemplo: "Juan Carlos" + "García" → "juangarcia"
     * Si ya existe, agrega sufijo numérico: "juangarcia2", "juangarcia3", etc.
     */
    private String generarUsername(String nombre, String apellidoPaterno) {
        String baseNombre   = normalizarSegmento(nombre);
        String baseApellido = normalizarSegmento(apellidoPaterno);
        String base = (baseNombre + baseApellido).trim();
        if (base.isBlank()) base = "usuario";

        if (userService.findByUsername(base).isEmpty()) return base;

        int suffix = 2;
        while (userService.findByUsername(base + suffix).isPresent()) suffix++;
        return base + suffix;
    }

    /**
     * Toma el primer token del texto, elimina acentos y deja solo alfanumérico en minúsculas.
     * Ejemplo: "García-López" → "garcia"
     */
    private String normalizarSegmento(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String primerToken = texto.trim().split("\\s+")[0];
        return Normalizer.normalize(primerToken, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")       // quitar diacríticos
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");   // solo alfanumérico
    }

    /**
     * Genera una contraseña de 12 caracteres que incluye mayúsculas, minúsculas,
     * dígitos y caracteres especiales. Usa SecureRandom para criptografía segura.
     */
    private String generarPasswordSeguro() {
        final String upper   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String lower   = "abcdefghijklmnopqrstuvwxyz";
        final String digits  = "0123456789";
        final String special = "!@#$%&*";
        final String all     = upper + lower + digits + special;

        SecureRandom rng = new SecureRandom();
        char[] pw = new char[12];

        // Garantizar al menos uno de cada tipo
        pw[0] = upper  .charAt(rng.nextInt(upper.length()));
        pw[1] = lower  .charAt(rng.nextInt(lower.length()));
        pw[2] = digits .charAt(rng.nextInt(digits.length()));
        pw[3] = special.charAt(rng.nextInt(special.length()));

        for (int i = 4; i < 12; i++) {
            pw[i] = all.charAt(rng.nextInt(all.length()));
        }

        // Mezclar (Fisher-Yates)
        for (int i = pw.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = pw[i]; pw[i] = pw[j]; pw[j] = tmp;
        }

        return new String(pw);
    }
}
