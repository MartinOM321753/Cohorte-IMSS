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
    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    // ── Migración: permisos deprecated → nuevos códigos ACCEDER/LOOKUP ────────
    //
    // Cuando se refactorizó el catálogo para separar "acceso a pantalla" de
    // "consulta cross-módulo", los códigos genéricos MODULO_VER pasaron a ser
    // deprecated. Cada uno se traduce al par (ACCEDER, LOOKUP) equivalente para
    // conservar el comportamiento previo tras la migración.
    private static final Map<String, List<String>> PERMISOS_DEPRECATED_MAP = Map.ofEntries(
            Map.entry("PACIENTES_VER",         List.of("PACIENTES_ACCEDER", "PACIENTES_LOOKUP")),
            Map.entry("ESTUDIOS_VER",          List.of("ESTUDIOS_LLENADO_ACCEDER", "ESTUDIOS_CATALOGO_ACCEDER", "ESTUDIOS_TIPOS_LOOKUP")),
            Map.entry("EXAMENES_VER",          List.of("EXAMENES_LLENADO_ACCEDER", "EXAMENES_CATALOGO_ACCEDER", "EXAMENES_LOOKUP")),
            Map.entry("REFRIGERADORES_VER",    List.of("REFRIGERADORES_ACCEDER", "REFRIGERADORES_LOOKUP")),
            Map.entry("CAJAS_VER",             List.of("CAJAS_ACCEDER", "CAJAS_LOOKUP")),
            Map.entry("TIPOS_MUESTRA_VER",     List.of("TIPOS_MUESTRA_ACCEDER", "TIPOS_MUESTRA_LOOKUP")),
            Map.entry("ESTUDIOS_MUESTRA_VER",  List.of("ESTUDIOS_MUESTRA_ACCEDER", "ESTUDIOS_MUESTRA_LOOKUP")),
            Map.entry("TRASLADOS_VER",         List.of("TRASLADOS_ACCEDER", "TRASLADOS_LOOKUP")),
            Map.entry("INSTITUCIONES_VER",     List.of("INSTITUCIONES_ACCEDER", "INSTITUCIONES_LOOKUP")),
            Map.entry("CATALOGOS_VER",         List.of("CATALOGOS_ACCEDER", "UNIDADES_LOOKUP")),
            Map.entry("USUARIOS_VER",          List.of("USUARIOS_ACCEDER", "USUARIOS_LOOKUP_MEDICOS"))
    );

    // ── Catálogo completo de permisos ──────────────────────────────────────────

    private static final List<String[]> CATALOGO_PERMISOS = List.of(
            // {codigo, modulo, descripcion}
            new String[]{"DASHBOARD_VER", "DASHBOARD", "Acceder al panel principal"},
            new String[]{"DASHBOARD_KPI_VER", "DASHBOARD", "Ver KPIs generales (participantes, citas, estudios, examenes)"},
            new String[]{"DASHBOARD_SOMATOMETRIA_VER", "DASHBOARD", "Ver graficos de somatometria"},
            new String[]{"DASHBOARD_EXAMENES_VER", "DASHBOARD", "Ver graficos de examenes de laboratorio"},
            new String[]{"DASHBOARD_BIOBANCO_VER", "DASHBOARD", "Ver ocupacion de biobanco"},
            new String[]{"DASHBOARD_AGENDA_VER", "DASHBOARD", "Ver agenda del dia"},

            new String[]{"PACIENTES_ACCEDER", "PACIENTES", "Acceder a la pantalla de participantes (sidebar + tabla completa)"},
            new String[]{"PACIENTES_LOOKUP", "PACIENTES", "Consultar participantes por busqueda/UUID desde formularios de otros modulos"},
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

            new String[]{"ESTUDIOS_LLENADO_ACCEDER", "ESTUDIOS", "Acceder al tab Llenado de estudios"},
            new String[]{"ESTUDIOS_CATALOGO_ACCEDER", "ESTUDIOS", "Acceder al tab Catalogo de tipos de estudio"},
            new String[]{"ESTUDIOS_TIPOS_LOOKUP", "ESTUDIOS", "Consultar tipos de estudio (dropdown en llenado/citas)"},
            new String[]{"ESTUDIOS_CREAR", "ESTUDIOS", "Registrar resultados de estudio (llenado)"},
            new String[]{"ESTUDIOS_EDITAR", "ESTUDIOS", "Editar resultados de estudio (llenado)"},
            new String[]{"ESTUDIOS_ELIMINAR", "ESTUDIOS", "Eliminar estudios (llenado)"},
            new String[]{"ESTUDIOS_TIPOS_CREAR", "ESTUDIOS", "Crear tipos de estudio en catalogo"},
            new String[]{"ESTUDIOS_TIPOS_EDITAR", "ESTUDIOS", "Editar tipos de estudio en catalogo"},
            new String[]{"ESTUDIOS_TIPOS_ELIMINAR", "ESTUDIOS", "Eliminar tipos de estudio en catalogo"},

            new String[]{"EXAMENES_LLENADO_ACCEDER", "EXAMENES", "Acceder al tab Llenado de examenes"},
            new String[]{"EXAMENES_CATALOGO_ACCEDER", "EXAMENES", "Acceder al tab Catalogo de examenes"},
            new String[]{"EXAMENES_LOOKUP", "EXAMENES", "Consultar examenes (dropdown en llenado)"},
            new String[]{"EXAMENES_CREAR", "EXAMENES", "Registrar resultados de examen (llenado)"},
            new String[]{"EXAMENES_EDITAR", "EXAMENES", "Editar resultados de examen (llenado)"},
            new String[]{"EXAMENES_ELIMINAR", "EXAMENES", "Eliminar resultados de examen (llenado)"},
            new String[]{"EXAMENES_CATALOGO_CREAR", "EXAMENES", "Crear examenes en catalogo"},
            new String[]{"EXAMENES_CATALOGO_EDITAR", "EXAMENES", "Editar examenes en catalogo"},
            new String[]{"EXAMENES_CATALOGO_ELIMINAR", "EXAMENES", "Eliminar examenes en catalogo"},

            new String[]{"CITAS_VER", "CITAS", "Ver citas"},
            new String[]{"CITAS_CREAR", "CITAS", "Crear citas"},
            new String[]{"CITAS_EDITAR", "CITAS", "Editar citas"},
            new String[]{"CITAS_ELIMINAR", "CITAS", "Eliminar citas"},
            new String[]{"CITAS_CONFIGURACION_VER", "CITAS", "Ver configuracion de horarios"},
            new String[]{"CITAS_CONFIGURACION_EDITAR", "CITAS", "Editar configuracion de horarios"},

            new String[]{"COBERTURA_VER", "COBERTURA", "Ver cobertura"},

            new String[]{"BIOBANCO_VER", "BIOBANCO", "Acceso a la pagina de biobanco"},

            new String[]{"REFRIGERADORES_ACCEDER", "REFRIGERADORES", "Acceder al tab Refrigeradores"},
            new String[]{"REFRIGERADORES_LOOKUP", "REFRIGERADORES", "Consultar refrigeradores y pisos desde formularios (CajaFormModal, etc.)"},
            new String[]{"REFRIGERADORES_CREAR", "REFRIGERADORES", "Crear refrigeradores"},
            new String[]{"REFRIGERADORES_EDITAR", "REFRIGERADORES", "Editar refrigeradores"},
            new String[]{"REFRIGERADORES_ELIMINAR", "REFRIGERADORES", "Eliminar refrigeradores"},

            new String[]{"CAJAS_ACCEDER", "CAJAS", "Acceder al tab Cajas criogenicas"},
            new String[]{"CAJAS_LOOKUP", "CAJAS", "Consultar cajas y posiciones desde formularios (asignar posicion)"},
            new String[]{"CAJAS_CREAR", "CAJAS", "Crear cajas criogenicas"},
            new String[]{"CAJAS_EDITAR", "CAJAS", "Editar cajas criogenicas"},
            new String[]{"CAJAS_ELIMINAR", "CAJAS", "Eliminar cajas criogenicas"},

            new String[]{"MUESTRAS_VER", "MUESTRAS", "Ver muestras"},
            new String[]{"MUESTRAS_CREAR", "MUESTRAS", "Crear muestras"},
            new String[]{"MUESTRAS_EDITAR", "MUESTRAS", "Editar muestras"},
            new String[]{"MUESTRAS_ELIMINAR", "MUESTRAS", "Eliminar muestras"},
            new String[]{"MUESTRAS_DAR_BAJA", "MUESTRAS", "Dar de baja muestras (irreversible)"},
            new String[]{"MUESTRAS_IMPRIMIR", "MUESTRAS", "Imprimir etiquetas de muestras"},

            new String[]{"TIPOS_MUESTRA_ACCEDER", "TIPOS_MUESTRA", "Acceder al tab admin de Tipos de muestra"},
            new String[]{"TIPOS_MUESTRA_LOOKUP", "TIPOS_MUESTRA", "Consultar tipos de muestra desde formularios (MuestraFormModal, etc.)"},
            new String[]{"TIPOS_MUESTRA_CREAR", "TIPOS_MUESTRA", "Crear tipos de muestra"},
            new String[]{"TIPOS_MUESTRA_EDITAR", "TIPOS_MUESTRA", "Editar tipos de muestra"},
            new String[]{"TIPOS_MUESTRA_ELIMINAR", "TIPOS_MUESTRA", "Eliminar tipos de muestra"},

            new String[]{"ESTUDIOS_MUESTRA_ACCEDER", "ESTUDIOS_MUESTRA", "Acceder al tab admin de Estudios de muestra"},
            new String[]{"ESTUDIOS_MUESTRA_LOOKUP", "ESTUDIOS_MUESTRA", "Consultar tipos de estudio de muestra (dropdown en llenado)"},
            new String[]{"ESTUDIOS_MUESTRA_CREAR", "ESTUDIOS_MUESTRA", "Crear estudios de muestra"},
            new String[]{"ESTUDIOS_MUESTRA_EDITAR", "ESTUDIOS_MUESTRA", "Editar estudios de muestra"},
            new String[]{"ESTUDIOS_MUESTRA_ELIMINAR", "ESTUDIOS_MUESTRA", "Eliminar estudios de muestra"},

            new String[]{"TRASLADOS_ACCEDER", "TRASLADOS", "Acceder al tab Prestamos entre instituciones"},
            new String[]{"TRASLADOS_LOOKUP", "TRASLADOS", "Consultar estado de traslados desde MuestrasTab"},
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

            new String[]{"USUARIOS_ACCEDER", "USUARIOS", "Acceder a la pantalla de usuarios (sidebar + tabla)"},
            new String[]{"USUARIOS_LOOKUP_MEDICOS", "USUARIOS", "Consultar usuarios con rol MEDICO desde formularios de citas/estudios"},
            new String[]{"USUARIOS_CREAR", "USUARIOS", "Crear usuarios"},
            new String[]{"USUARIOS_EDITAR", "USUARIOS", "Editar usuarios"},
            new String[]{"USUARIOS_ELIMINAR", "USUARIOS", "Eliminar usuarios"},

            new String[]{"INSTITUCIONES_ACCEDER", "INSTITUCIONES", "Acceder a la pantalla de instituciones"},
            new String[]{"INSTITUCIONES_LOOKUP", "INSTITUCIONES", "Consultar instituciones desde formularios (jerarquia, dropdowns)"},
            new String[]{"INSTITUCIONES_CREAR", "INSTITUCIONES", "Crear instituciones"},
            new String[]{"INSTITUCIONES_EDITAR", "INSTITUCIONES", "Editar instituciones"},
            new String[]{"INSTITUCIONES_ELIMINAR", "INSTITUCIONES", "Eliminar instituciones"},

            new String[]{"CATALOGOS_ACCEDER", "CATALOGOS", "Acceder a la pantalla de catalogos"},
            new String[]{"UNIDADES_LOOKUP", "CATALOGOS", "Consultar unidades de medida desde formularios de parametros"},
            new String[]{"CATALOGOS_EDITAR", "CATALOGOS", "Editar catalogos y copiar entre instituciones"},

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

    // Los conjuntos de abajo reflejan la configuracion que se curo rol por rol en
    // la aplicacion y quedo validada en uso. Son la semilla de una instalacion
    // nueva: insertarPermisos()/insertarRolPermisos() solo siembran un rol que no
    // tenga ningun permiso, asi que una base ya configurada NO se sobreescribe.
    //
    // Al cambiar algo aqui, cambiar tambien lo que corresponda en el catalogo
    // CATALOGO_PERMISOS: un codigo que no exista ahi se ignora silenciosamente.

    private static final Set<String> PERMISOS_RECEPCIONISTA = Set.of(
            "CITAS_CONFIGURACION_VER", "CITAS_CREAR", "CITAS_EDITAR",
            "CITAS_ELIMINAR", "CITAS_VER", "DASHBOARD_AGENDA_VER",
            "DASHBOARD_KPI_VER", "DASHBOARD_VER", "DOCUMENTOS_SUBIR",
            "DOCUMENTOS_VER_METADATA", "ESTUDIOS_LLENADO_ACCEDER", "ESTUDIOS_TIPOS_LOOKUP",
            "EXAMENES_LLENADO_ACCEDER", "EXAMENES_LOOKUP", "EXPEDIENTE_CITAS",
            "EXPEDIENTE_DATOS_PERSONALES", "EXPEDIENTE_DOCUMENTOS", "EXPEDIENTE_ESTUDIOS",
            "EXPEDIENTE_EXAMENES", "EXPEDIENTE_SOMATOMETRIA", "EXPEDIENTE_VER",
            "INSTITUCIONES_LOOKUP", "PACIENTES_ACCEDER", "PACIENTES_CREAR_ACCESO",
            "PACIENTES_LOOKUP", "SOMATOMETRIA_CREAR", "SOMATOMETRIA_EDITAR",
            "SOMATOMETRIA_VER", "USUARIOS_LOOKUP_MEDICOS"
    );

    private static final Set<String> PERMISOS_MEDICO = Set.of(
            "CITAS_CONFIGURACION_VER", "COBERTURA_VER", "DASHBOARD_AGENDA_VER",
            "DASHBOARD_EXAMENES_VER", "DASHBOARD_KPI_VER", "DASHBOARD_SOMATOMETRIA_VER",
            "DASHBOARD_VER", "DOCUMENTOS_DESCARGAR", "DOCUMENTOS_SUBIR",
            "DOCUMENTOS_VER_METADATA", "ESTUDIOS_CREAR", "ESTUDIOS_EDITAR",
            "ESTUDIOS_LLENADO_ACCEDER", "ESTUDIOS_TIPOS_LOOKUP", "EXAMENES_CREAR",
            "EXAMENES_EDITAR", "EXAMENES_LLENADO_ACCEDER", "EXAMENES_LOOKUP",
            "EXPEDIENTE_BIOBANCO", "EXPEDIENTE_CITAS", "EXPEDIENTE_DATOS_PERSONALES",
            "EXPEDIENTE_DOCUMENTOS", "EXPEDIENTE_ESTUDIOS", "EXPEDIENTE_EXAMENES",
            "EXPEDIENTE_PERFIL_LAB", "EXPEDIENTE_PRUEBA_ESCALON", "EXPEDIENTE_RESULTADOS_LAB",
            "EXPEDIENTE_SOMATOMETRIA", "EXPEDIENTE_VER", "INSTITUCIONES_LOOKUP",
            "PACIENTES_ACCEDER", "PACIENTES_CREAR", "PACIENTES_EDITAR",
            "PACIENTES_LOOKUP", "SOMATOMETRIA_CREAR", "SOMATOMETRIA_VER",
            "UNIDADES_LOOKUP"
    );

    private static final Set<String> PERMISOS_LABORATORISTA = Set.of(
            "BIOBANCO_VER", "CAJAS_ACCEDER", "CAJAS_CREAR",
            "CAJAS_EDITAR", "CAJAS_LOOKUP", "CITAS_CONFIGURACION_VER",
            "COBERTURA_VER", "DASHBOARD_AGENDA_VER", "DASHBOARD_BIOBANCO_VER",
            "DASHBOARD_EXAMENES_VER", "DASHBOARD_KPI_VER", "DASHBOARD_SOMATOMETRIA_VER",
            "DASHBOARD_VER", "DOCUMENTOS_DESCARGAR", "DOCUMENTOS_SUBIR",
            "DOCUMENTOS_VER_METADATA", "ESTUDIOS_MUESTRA_ACCEDER", "ESTUDIOS_MUESTRA_CREAR",
            "ESTUDIOS_MUESTRA_EDITAR", "ESTUDIOS_MUESTRA_LOOKUP", "INSTITUCIONES_LOOKUP",
            "MUESTRAS_CREAR", "MUESTRAS_DAR_BAJA", "MUESTRAS_EDITAR",
            "MUESTRAS_IMPRIMIR", "MUESTRAS_VER", "PACIENTES_LOOKUP",
            "REFRIGERADORES_ACCEDER", "REFRIGERADORES_CREAR", "REFRIGERADORES_EDITAR",
            "REFRIGERADORES_LOOKUP", "TIPOS_MUESTRA_ACCEDER", "TIPOS_MUESTRA_CREAR",
            "TIPOS_MUESTRA_EDITAR", "TIPOS_MUESTRA_LOOKUP", "TRASLADOS_ACCEDER",
            "TRASLADOS_CANCELAR", "TRASLADOS_CONFIRMAR", "TRASLADOS_CREAR",
            "TRASLADOS_DEVOLVER", "TRASLADOS_LOOKUP", "UNIDADES_LOOKUP"
    );

    private static final Set<String> PERMISOS_ENCARGADO = Set.of(
            "BIOBANCO_VER", "CAJAS_ACCEDER", "CAJAS_CREAR",
            "CAJAS_EDITAR", "CAJAS_ELIMINAR", "CAJAS_LOOKUP",
            "CITAS_CONFIGURACION_VER", "DOCUMENTOS_DESCARGAR", "DOCUMENTOS_ELIMINAR",
            "DOCUMENTOS_SUBIR", "DOCUMENTOS_VER_METADATA", "ESTUDIOS_MUESTRA_ACCEDER",
            "ESTUDIOS_MUESTRA_CREAR", "ESTUDIOS_MUESTRA_EDITAR", "ESTUDIOS_MUESTRA_ELIMINAR",
            "ESTUDIOS_MUESTRA_LOOKUP", "INSTITUCIONES_LOOKUP", "MUESTRAS_CREAR",
            "MUESTRAS_EDITAR", "MUESTRAS_ELIMINAR", "MUESTRAS_IMPRIMIR",
            "MUESTRAS_VER", "PACIENTES_LOOKUP", "REFRIGERADORES_ACCEDER",
            "REFRIGERADORES_CREAR", "REFRIGERADORES_EDITAR", "REFRIGERADORES_ELIMINAR",
            "REFRIGERADORES_LOOKUP", "TIPOS_MUESTRA_ACCEDER", "TIPOS_MUESTRA_CREAR",
            "TIPOS_MUESTRA_EDITAR", "TIPOS_MUESTRA_ELIMINAR", "TIPOS_MUESTRA_LOOKUP",
            "TRASLADOS_ACCEDER", "TRASLADOS_CANCELAR", "TRASLADOS_CONFIRMAR",
            "TRASLADOS_CREAR", "TRASLADOS_DEVOLVER", "TRASLADOS_LOOKUP",
            "UNIDADES_LOOKUP"
    );

    private static final Set<String> PERMISOS_PACIENTE = Set.of(
            "DOCUMENTOS_VER_METADATA", "EXPEDIENTE_CITAS", "EXPEDIENTE_DATOS_PERSONALES",
            "EXPEDIENTE_DOCUMENTOS", "EXPEDIENTE_ESTUDIOS", "EXPEDIENTE_EXAMENES",
            "EXPEDIENTE_VER"
    );

    private static final Map<String, Set<String>> ROL_PERMISOS_MAP = Map.of(
            "ROOT", allPermisoCodes(),
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
        migrarPermisosDeprecatedEnRoles();
        migrarPermisosDeprecatedEnUsuarios();
        desactivarPermisosDeprecated();
        insertarRolPermisos();
        migrarUsuariosExistentes();
        revocarDashboardDePaciente();
    }

    /**
     * Para cada rol_permiso que apunta a un código deprecated, agrega los
     * códigos nuevos equivalentes (si no existen aún) y elimina el registro
     * deprecated. Idempotente: en ejecuciones sucesivas, cuando los códigos
     * deprecated ya no aparezcan en rol_permiso, esto no hace nada.
     */
    private void migrarPermisosDeprecatedEnRoles() {
        int actualizados = 0;
        for (Map.Entry<String, List<String>> entry : PERMISOS_DEPRECATED_MAP.entrySet()) {
            String codigoDeprecated = entry.getKey();
            List<String> nuevosCodigos = entry.getValue();

            Optional<Permiso> optDeprecated = permisoRepository.findByCodigo(codigoDeprecated);
            if (optDeprecated.isEmpty()) continue;
            Permiso deprecated = optDeprecated.get();

            List<Permiso> nuevosPermisos = nuevosCodigos.stream()
                    .map(permisoRepository::findByCodigo)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            // Recorrer todos los roles que tienen el permiso deprecated
            List<Role> roles = roleRepository.findAll();
            for (Role rol : roles) {
                if (!rolPermisoRepository.existsByRolAndPermiso(rol, deprecated)) continue;

                for (Permiso nuevo : nuevosPermisos) {
                    if (!rolPermisoRepository.existsByRolAndPermiso(rol, nuevo)) {
                        RolPermiso rp = new RolPermiso();
                        rp.setRol(rol);
                        rp.setPermiso(nuevo);
                        rolPermisoRepository.save(rp);
                    }
                }
                rolPermisoRepository.deleteByRolAndPermiso(rol, deprecated);
                actualizados++;
            }
        }
        if (actualizados > 0) {
            log.info("PermisoInitializer: {} rol_permiso migrados de codigos deprecated a ACCEDER/LOOKUP.", actualizados);
        }
    }

    /**
     * Para cada usuario_permiso (concesión o restricción individual) que apunta
     * a un código deprecated, crea entradas equivalentes con los nuevos códigos
     * conservando tipo, motivo, fechas y desactiva la entrada deprecated.
     */
    private void migrarPermisosDeprecatedEnUsuarios() {
        int migrados = 0;
        for (Map.Entry<String, List<String>> entry : PERMISOS_DEPRECATED_MAP.entrySet()) {
            String codigoDeprecated = entry.getKey();
            List<String> nuevosCodigos = entry.getValue();

            Optional<Permiso> optDeprecated = permisoRepository.findByCodigo(codigoDeprecated);
            if (optDeprecated.isEmpty()) continue;
            Permiso deprecated = optDeprecated.get();

            List<Permiso> nuevosPermisos = nuevosCodigos.stream()
                    .map(permisoRepository::findByCodigo)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();

            List<UsuarioPermiso> concesiones = usuarioPermisoRepository.findAllByPermiso(deprecated);
            for (UsuarioPermiso up : concesiones) {
                if (!Boolean.TRUE.equals(up.getActivo())) continue;

                for (Permiso nuevo : nuevosPermisos) {
                    UsuarioPermiso replica = new UsuarioPermiso();
                    replica.setUsuario(up.getUsuario());
                    replica.setPermiso(nuevo);
                    replica.setTipo(up.getTipo());
                    replica.setMotivo(up.getMotivo());
                    replica.setFechaInicio(up.getFechaInicio());
                    replica.setFechaFin(up.getFechaFin());
                    replica.setOtorgadoPor(up.getOtorgadoPor());
                    replica.setFechaCreacion(LocalDateTime.now());
                    replica.setActivo(true);
                    usuarioPermisoRepository.save(replica);
                }
                up.setActivo(false);
                usuarioPermisoRepository.save(up);
                migrados++;
            }
        }
        if (migrados > 0) {
            log.info("PermisoInitializer: {} usuario_permiso migrados de codigos deprecated.", migrados);
        }
    }

    /**
     * Marca los permisos deprecated como activo=false para que no aparezcan en
     * el catálogo del panel admin. Se mantienen en BD por trazabilidad.
     */
    private void desactivarPermisosDeprecated() {
        int desactivados = 0;
        for (String codigoDeprecated : PERMISOS_DEPRECATED_MAP.keySet()) {
            Optional<Permiso> opt = permisoRepository.findByCodigo(codigoDeprecated);
            if (opt.isEmpty()) continue;
            Permiso p = opt.get();
            if (Boolean.TRUE.equals(p.getActivo())) {
                p.setActivo(false);
                permisoRepository.save(p);
                desactivados++;
            }
        }
        if (desactivados > 0) {
            log.info("PermisoInitializer: {} permisos deprecated marcados como activo=false.", desactivados);
        }
    }

    /**
     * DASHBOARD_VER se removió del catálogo de permisos de PACIENTE (enrutaba
     * al dashboard de staff, que dispara consultas sin autorización para ese
     * rol). Revoca el mapeo si quedó sembrado por una ejecución previa.
     */
    private void revocarDashboardDePaciente() {
        Optional<Role> optRolPaciente = roleRepository.findByRole("PACIENTE");
        Optional<Permiso> optDashboard = permisoRepository.findByCodigo("DASHBOARD_VER");
        if (optRolPaciente.isEmpty() || optDashboard.isEmpty()) return;

        if (rolPermisoRepository.existsByRolAndPermiso(optRolPaciente.get(), optDashboard.get())) {
            rolPermisoRepository.deleteByRolAndPermiso(optRolPaciente.get(), optDashboard.get());
            log.info("PermisoInitializer: revocado DASHBOARD_VER del rol PACIENTE.");
        }
    }

    private void garantizarRolPaciente() {
        if (roleRepository.findByRole("PACIENTE").isPresent()) return;

        Role paciente = new Role();
        paciente.setRole("PACIENTE");
        paciente.setUuid(UUID.randomUUID().toString());
        roleRepository.save(paciente);
        log.info("PermisoInitializer: rol PACIENTE creado.");
    }

    private final Set<String> permisosRecienCreados = new HashSet<>();

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
                permisosRecienCreados.add(def[0]);
                nuevos++;
            }
        }
        if (nuevos > 0) {
            log.info("PermisoInitializer: {} permisos insertados.", nuevos);
        }
    }

    /**
     * Dos modos de operación:
     * 1. Rol vacío (sin permisos): siembra TODOS los permisos por defecto del mapa.
     * 2. Rol existente (ya tiene permisos): solo agrega permisos RECIÉN CREADOS
     *    en esta ejecución, para que las nuevas features se propaguen sin
     *    sobreescribir cambios hechos en producción.
     */
    private void insertarRolPermisos() {
        int nuevos = 0;
        for (Map.Entry<String, Set<String>> entry : ROL_PERMISOS_MAP.entrySet()) {
            Optional<Role> optRole = roleRepository.findByRole(entry.getKey());
            if (optRole.isEmpty()) continue;

            Role rol = optRole.get();
            boolean rolVacio = rolPermisoRepository.countByRol(rol) == 0;

            Set<String> codigos = rolVacio
                    ? entry.getValue()
                    : entry.getValue().stream()
                        .filter(permisosRecienCreados::contains)
                        .collect(Collectors.toSet());

            for (String codigoPermiso : codigos) {
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

            if (rolVacio) {
                log.info("PermisoInitializer: rol '{}' sembrado con {} permisos por defecto.", entry.getKey(), codigos.size());
            }
        }
        if (nuevos > 0) {
            log.info("PermisoInitializer: {} mapeos rol-permiso insertados en total.", nuevos);
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
