package imss.gob.mx.cohorte.modules.permisos;

import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class PermisoInitializer {

    private final PermisoRepository permisoRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    // ── Catálogo completo de permisos ──────────────────────────────────────────

    private static final List<String[]> CATALOGO_PERMISOS = List.of(
            // {codigo, modulo, descripcion}
            new String[]{"DASHBOARD_VER", "DASHBOARD", "Ver panel principal"},

            new String[]{"PACIENTES_VER", "PACIENTES", "Ver lista de participantes"},
            new String[]{"PACIENTES_CREAR", "PACIENTES", "Crear nuevos participantes"},
            new String[]{"PACIENTES_EDITAR", "PACIENTES", "Editar datos de participantes"},
            new String[]{"PACIENTES_ELIMINAR", "PACIENTES", "Eliminar participantes"},
            new String[]{"PACIENTES_IMPORTAR", "PACIENTES", "Importar participantes masivamente"},
            new String[]{"PACIENTES_CREAR_ACCESO", "PACIENTES", "Crear cuenta de acceso para participantes"},

            new String[]{"EXPEDIENTE_VER", "EXPEDIENTE", "Ver expediente de participante"},
            new String[]{"EXPEDIENTE_DATOS_PERSONALES", "EXPEDIENTE", "Ver datos personales del expediente"},
            new String[]{"EXPEDIENTE_SOMATOMETRIA", "EXPEDIENTE", "Ver somatometria del expediente"},
            new String[]{"EXPEDIENTE_RESULTADOS_LAB", "EXPEDIENTE", "Ver resultados de laboratorio"},
            new String[]{"EXPEDIENTE_PERFIL_LAB", "EXPEDIENTE", "Ver perfil de laboratorio"},
            new String[]{"EXPEDIENTE_BIOBANCO", "EXPEDIENTE", "Ver seccion biobanco del expediente"},
            new String[]{"EXPEDIENTE_PRUEBA_ESCALON", "EXPEDIENTE", "Ver prueba de escalon"},
            new String[]{"EXPEDIENTE_CITAS", "EXPEDIENTE", "Ver citas del expediente"},
            new String[]{"EXPEDIENTE_ESTUDIOS", "EXPEDIENTE", "Ver estudios del expediente"},
            new String[]{"EXPEDIENTE_EXAMENES", "EXPEDIENTE", "Ver examenes del expediente"},
            new String[]{"EXPEDIENTE_DOCUMENTOS", "EXPEDIENTE", "Ver documentos del expediente"},

            new String[]{"ESTUDIOS_VER", "ESTUDIOS", "Ver estudios medicos"},
            new String[]{"ESTUDIOS_CREAR", "ESTUDIOS", "Crear estudios medicos"},
            new String[]{"ESTUDIOS_EDITAR", "ESTUDIOS", "Editar estudios medicos"},
            new String[]{"ESTUDIOS_ELIMINAR", "ESTUDIOS", "Eliminar estudios medicos"},

            new String[]{"EXAMENES_VER", "EXAMENES", "Ver examenes"},
            new String[]{"EXAMENES_CREAR", "EXAMENES", "Crear examenes"},
            new String[]{"EXAMENES_EDITAR", "EXAMENES", "Editar examenes"},
            new String[]{"EXAMENES_ELIMINAR", "EXAMENES", "Eliminar examenes"},

            new String[]{"CITAS_VER", "CITAS", "Ver citas"},
            new String[]{"CITAS_CREAR", "CITAS", "Crear citas"},
            new String[]{"CITAS_EDITAR", "CITAS", "Editar citas"},
            new String[]{"CITAS_ELIMINAR", "CITAS", "Eliminar citas"},
            new String[]{"CITAS_CONFIGURACION_VER", "CITAS", "Ver configuracion de horarios"},
            new String[]{"CITAS_CONFIGURACION_EDITAR", "CITAS", "Editar configuracion de horarios"},

            new String[]{"COBERTURA_VER", "COBERTURA", "Ver cobertura"},

            new String[]{"BIOBANCO_VER", "BIOBANCO", "Ver biobanco"},
            new String[]{"BIOBANCO_CREAR", "BIOBANCO", "Crear elementos de biobanco"},
            new String[]{"BIOBANCO_EDITAR", "BIOBANCO", "Editar elementos de biobanco"},
            new String[]{"BIOBANCO_ELIMINAR", "BIOBANCO", "Eliminar elementos de biobanco"},

            new String[]{"MUESTRAS_VER", "MUESTRAS", "Ver muestras"},
            new String[]{"MUESTRAS_CREAR", "MUESTRAS", "Crear muestras"},
            new String[]{"MUESTRAS_EDITAR", "MUESTRAS", "Editar muestras"},
            new String[]{"MUESTRAS_ELIMINAR", "MUESTRAS", "Eliminar muestras"},
            new String[]{"MUESTRAS_IMPRIMIR", "MUESTRAS", "Imprimir etiquetas de muestras"},

            new String[]{"TRASLADOS_VER", "TRASLADOS", "Ver traslados de muestras"},
            new String[]{"TRASLADOS_CREAR", "TRASLADOS", "Crear traslados"},
            new String[]{"TRASLADOS_CONFIRMAR", "TRASLADOS", "Confirmar recepcion de traslados"},
            new String[]{"TRASLADOS_DEVOLVER", "TRASLADOS", "Iniciar devolucion de traslados"},
            new String[]{"TRASLADOS_CANCELAR", "TRASLADOS", "Cancelar traslados"},

            new String[]{"DOCUMENTOS_VER_METADATA", "DOCUMENTOS", "Ver metadata de documentos"},
            new String[]{"DOCUMENTOS_DESCARGAR", "DOCUMENTOS", "Descargar archivos de MinIO"},
            new String[]{"DOCUMENTOS_SUBIR", "DOCUMENTOS", "Subir documentos"},
            new String[]{"DOCUMENTOS_ELIMINAR", "DOCUMENTOS", "Eliminar documentos"},

            new String[]{"SOMATOMETRIA_VER", "SOMATOMETRIA", "Ver datos somatometricos"},
            new String[]{"SOMATOMETRIA_CREAR", "SOMATOMETRIA", "Crear registros somatometricos"},
            new String[]{"SOMATOMETRIA_EDITAR", "SOMATOMETRIA", "Editar datos somatometricos"},
            new String[]{"SOMATOMETRIA_ELIMINAR", "SOMATOMETRIA", "Eliminar datos somatometricos"},

            new String[]{"USUARIOS_VER", "USUARIOS", "Ver lista de usuarios"},
            new String[]{"USUARIOS_CREAR", "USUARIOS", "Crear usuarios"},
            new String[]{"USUARIOS_EDITAR", "USUARIOS", "Editar usuarios"},
            new String[]{"USUARIOS_ELIMINAR", "USUARIOS", "Eliminar usuarios"},

            new String[]{"INSTITUCIONES_VER", "INSTITUCIONES", "Ver instituciones"},
            new String[]{"INSTITUCIONES_CREAR", "INSTITUCIONES", "Crear instituciones"},
            new String[]{"INSTITUCIONES_EDITAR", "INSTITUCIONES", "Editar instituciones"},
            new String[]{"INSTITUCIONES_ELIMINAR", "INSTITUCIONES", "Eliminar instituciones"},

            new String[]{"CATALOGOS_VER", "CATALOGOS", "Ver catalogos"},
            new String[]{"CATALOGOS_EDITAR", "CATALOGOS", "Editar catalogos"},

            new String[]{"CONFIGURACION_VER", "CONFIGURACION", "Ver configuracion"},
            new String[]{"CONFIGURACION_EDITAR", "CONFIGURACION", "Editar configuracion"},

            new String[]{"BITACORA_ACCESOS_VER", "BITACORA", "Ver bitacora de accesos"},
            new String[]{"BITACORA_ACCIONES_VER", "BITACORA", "Ver bitacora de acciones"},

            new String[]{"PERMISOS_VER", "PERMISOS", "Ver panel de permisos"},
            new String[]{"PERMISOS_EDITAR", "PERMISOS", "Editar permisos y roles"}
    );

    // ── Mapeo de roles → códigos de permisos ──────────────────────────────────

    private static Set<String> allPermisoCodes() {
        return CATALOGO_PERMISOS.stream()
                .map(arr -> arr[0])
                .collect(Collectors.toSet());
    }

    private static final Set<String> PERMISOS_ADMINISTRADOR = allPermisoCodes();

    private static final Set<String> PERMISOS_RECEPCIONISTA = Set.of(
            "DASHBOARD_VER",
            "PACIENTES_VER", "PACIENTES_CREAR", "PACIENTES_EDITAR", "PACIENTES_CREAR_ACCESO",
            "EXPEDIENTE_VER", "EXPEDIENTE_DATOS_PERSONALES", "EXPEDIENTE_SOMATOMETRIA",
            "EXPEDIENTE_CITAS", "EXPEDIENTE_ESTUDIOS", "EXPEDIENTE_EXAMENES", "EXPEDIENTE_DOCUMENTOS",
            "ESTUDIOS_VER", "ESTUDIOS_CREAR", "ESTUDIOS_EDITAR",
            "EXAMENES_VER", "EXAMENES_CREAR", "EXAMENES_EDITAR",
            "CITAS_VER", "CITAS_CREAR", "CITAS_EDITAR",
            "DOCUMENTOS_VER_METADATA", "DOCUMENTOS_DESCARGAR", "DOCUMENTOS_SUBIR",
            "SOMATOMETRIA_VER", "SOMATOMETRIA_CREAR", "SOMATOMETRIA_EDITAR"
    );

    private static final Set<String> PERMISOS_MEDICO = Set.of(
            "DASHBOARD_VER",
            "PACIENTES_VER", "PACIENTES_CREAR", "PACIENTES_EDITAR",
            "EXPEDIENTE_VER", "EXPEDIENTE_DATOS_PERSONALES", "EXPEDIENTE_SOMATOMETRIA",
            "EXPEDIENTE_RESULTADOS_LAB", "EXPEDIENTE_PERFIL_LAB", "EXPEDIENTE_BIOBANCO",
            "EXPEDIENTE_PRUEBA_ESCALON", "EXPEDIENTE_CITAS", "EXPEDIENTE_ESTUDIOS",
            "EXPEDIENTE_EXAMENES", "EXPEDIENTE_DOCUMENTOS",
            "ESTUDIOS_VER", "ESTUDIOS_CREAR", "ESTUDIOS_EDITAR",
            "EXAMENES_VER", "EXAMENES_CREAR", "EXAMENES_EDITAR",
            "CITAS_VER", "CITAS_CREAR", "CITAS_EDITAR",
            "COBERTURA_VER",
            "BIOBANCO_VER", "MUESTRAS_VER",
            "DOCUMENTOS_VER_METADATA", "DOCUMENTOS_DESCARGAR", "DOCUMENTOS_SUBIR",
            "SOMATOMETRIA_VER", "SOMATOMETRIA_CREAR", "SOMATOMETRIA_EDITAR"
    );

    private static final Set<String> PERMISOS_LABORATORISTA = Set.of(
            "DASHBOARD_VER",
            "EXPEDIENTE_RESULTADOS_LAB", "EXPEDIENTE_PERFIL_LAB",
            "EXPEDIENTE_EXAMENES", "EXPEDIENTE_BIOBANCO", "EXPEDIENTE_DOCUMENTOS",
            "EXAMENES_VER", "EXAMENES_CREAR", "EXAMENES_EDITAR",
            "BIOBANCO_VER", "BIOBANCO_CREAR", "BIOBANCO_EDITAR",
            "MUESTRAS_VER", "MUESTRAS_CREAR", "MUESTRAS_EDITAR", "MUESTRAS_ELIMINAR", "MUESTRAS_IMPRIMIR",
            "DOCUMENTOS_VER_METADATA", "DOCUMENTOS_DESCARGAR", "DOCUMENTOS_SUBIR"
    );

    private static final Set<String> PERMISOS_ENCARGADO = Set.of(
            "MUESTRAS_VER",
            "TRASLADOS_VER", "TRASLADOS_CREAR", "TRASLADOS_CONFIRMAR",
            "TRASLADOS_DEVOLVER", "TRASLADOS_CANCELAR",
            "BIOBANCO_VER",
            "DOCUMENTOS_VER_METADATA"
    );

    private static final Set<String> PERMISOS_PACIENTE = Set.of(
            "DASHBOARD_VER",
            "EXPEDIENTE_VER", "EXPEDIENTE_DATOS_PERSONALES",
            "EXPEDIENTE_CITAS", "EXPEDIENTE_ESTUDIOS",
            "EXPEDIENTE_EXAMENES", "EXPEDIENTE_DOCUMENTOS",
            "DOCUMENTOS_VER_METADATA"
    );

    private static final Map<String, Set<String>> ROL_PERMISOS_MAP = Map.of(
            "ADMINISTRADOR", PERMISOS_ADMINISTRADOR,
            "RECEPCIONISTA", PERMISOS_RECEPCIONISTA,
            "MEDICO", PERMISOS_MEDICO,
            "LABORATORISTA", PERMISOS_LABORATORISTA,
            "ENCARGADO", PERMISOS_ENCARGADO,
            "PACIENTE", PERMISOS_PACIENTE
    );

    // ── Ejecución ─────────────────────────────────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void inicializar() {
        garantizarRolPaciente();
        insertarPermisos();
        insertarRolPermisos();
        migrarUsuariosExistentes();
    }

    private void garantizarRolPaciente() {
        if (roleRepository.findByRole("PACIENTE").isPresent()) return;

        Role paciente = new Role();
        paciente.setRole("PACIENTE");
        paciente.setUuid(UUID.randomUUID().toString());
        roleRepository.save(paciente);
        log.info("PermisoInitializer: rol PACIENTE creado.");
    }

    private void insertarPermisos() {
        int nuevos = 0;
        for (String[] def : CATALOGO_PERMISOS) {
            if (!permisoRepository.existsByCodigo(def[0])) {
                Permiso p = new Permiso();
                p.setCodigo(def[0]);
                p.setModulo(def[1]);
                p.setDescripcion(def[2]);
                p.setActivo(true);
                permisoRepository.save(p);
                nuevos++;
            }
        }
        if (nuevos > 0) {
            log.info("PermisoInitializer: {} permisos insertados.", nuevos);
        }
    }

    private void insertarRolPermisos() {
        int nuevos = 0;
        for (Map.Entry<String, Set<String>> entry : ROL_PERMISOS_MAP.entrySet()) {
            Optional<Role> optRole = roleRepository.findByRole(entry.getKey());
            if (optRole.isEmpty()) continue;

            Role rol = optRole.get();
            for (String codigoPermiso : entry.getValue()) {
                Optional<Permiso> optPermiso = permisoRepository.findByCodigo(codigoPermiso);
                if (optPermiso.isEmpty()) continue;

                Permiso permiso = optPermiso.get();
                if (!rolPermisoRepository.existsByRolAndPermiso(rol, permiso)) {
                    RolPermiso rp = new RolPermiso();
                    rp.setRol(rol);
                    rp.setPermiso(permiso);
                    rolPermisoRepository.save(rp);
                    nuevos++;
                }
            }
        }
        if (nuevos > 0) {
            log.info("PermisoInitializer: {} mapeos rol-permiso insertados.", nuevos);
        }
    }

    @SuppressWarnings("deprecation")
    private void migrarUsuariosExistentes() {
        int migrados = 0;
        List<BeanUser> usuarios = userRepository.findAll();
        for (BeanUser usuario : usuarios) {
            if (usuarioRolRepository.countByUsuario(usuario) > 0) continue;

            Role rolLegacy = usuario.getRol();
            if (rolLegacy == null) continue;

            UsuarioRol ur = new UsuarioRol();
            ur.setUsuario(usuario);
            ur.setRol(rolLegacy);
            ur.setFechaAsignacion(LocalDateTime.now());
            usuarioRolRepository.save(ur);
            migrados++;
        }
        if (migrados > 0) {
            log.info("PermisoInitializer: {} usuarios migrados a usuario_rol.", migrados);
        }
    }
}
