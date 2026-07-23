package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.permisos.dto.*;
import imss.gob.mx.cohorte.modules.permisos.*;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.permisos.PermisoEvaluationService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermisoAdminApplicationService {

    private final PermisoRepository permisoRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final BitacoraPermisosRepository bitacoraRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final InstitucionContextService institucionCtx;
    private final PermisoEvaluationService evaluationService;

    private static final String ROOT_ROLE = "ROOT";

    private boolean isCallerRoot() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ROOT"::equals);
    }

    // ── Catálogo de permisos ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, List<PermisoDTO>> getCatalogoAgrupado() {
        return permisoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .map(this::toPermisoDTO)
                .collect(Collectors.groupingBy(
                        PermisoDTO::getModulo,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    // ── Roles con sus permisos plantilla ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RolConPermisosDTO> getRolesConPermisos() {
        boolean callerIsRoot = isCallerRoot();
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
            .filter(rol -> callerIsRoot || !ROOT_ROLE.equals(rol.getRole()))
            .map(rol -> {
            List<PermisoDTO> permisos = rolPermisoRepository.findAllByRol(rol).stream()
                    .map(rp -> toPermisoDTO(rp.getPermiso()))
                    .collect(Collectors.toList());
            return RolConPermisosDTO.builder()
                    .id(rol.getId())
                    .uuid(rol.getUuid())
                    .nombre(rol.getRole())
                    .permisos(permisos)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public RolConPermisosDTO actualizarPermisosRol(Long idRol, List<String> codigosPermisos) {
        Role rol = roleRepository.findById(idRol)
                .orElseThrow(() -> new ObjNotFoundException("Rol no encontrado con id: " + idRol));

        if (ROOT_ROLE.equals(rol.getRole()) && !isCallerRoot()) {
            throw new ValidationException("No tiene permisos para modificar este rol");
        }

        rolPermisoRepository.deleteAllByRol(rol);
        rolPermisoRepository.flush();

        List<PermisoDTO> nuevosPermisos = new ArrayList<>();
        for (String codigo : codigosPermisos) {
            Permiso permiso = permisoRepository.findByCodigo(codigo)
                    .orElseThrow(() -> new ObjNotFoundException("Permiso no encontrado: " + codigo));
            RolPermiso rp = new RolPermiso();
            rp.setRol(rol);
            rp.setPermiso(permiso);
            rolPermisoRepository.save(rp);
            nuevosPermisos.add(toPermisoDTO(permiso));
        }

        registrarBitacora(null, "PERMISOS_ROL_ACTUALIZADOS",
                "Rol " + rol.getRole() + ": " + codigosPermisos.size() + " permisos asignados");

        return RolConPermisosDTO.builder()
                .id(rol.getId())
                .uuid(rol.getUuid())
                .nombre(rol.getRole())
                .permisos(nuevosPermisos)
                .build();
    }

    // ── Permisos efectivos de un usuario ────────────────────────────────────────

    @Transactional(readOnly = true)
    public UsuarioPermisosResumenDTO getPermisosUsuario(String uuid) {
        BeanUser usuario = findUsuario(uuid);

        List<RolAsignadoDTO> roles = usuarioRolRepository.findAllByUsuario(usuario).stream()
                .map(ur -> RolAsignadoDTO.builder()
                        .idUsuarioRol(ur.getId())
                        .idRol(ur.getRol().getId())
                        .uuid(ur.getRol().getUuid())
                        .nombre(ur.getRol().getRole())
                        .fechaAsignacion(ur.getFechaAsignacion())
                        .build())
                .collect(Collectors.toList());

        List<PermisoIndividualDTO> individuales = usuarioPermisoRepository
                .findAllByUsuarioAndActivoTrue(usuario).stream()
                .map(this::toPermisoIndividualDTO)
                .collect(Collectors.toList());

        List<PermisoEfectivoDTO> efectivos = buildPermisosEfectivos(usuario);

        return UsuarioPermisosResumenDTO.builder()
                .uuid(usuario.getUUID())
                .username(usuario.getUsername())
                .nombreCompleto(buildNombre(usuario))
                .roles(roles)
                .permisosEfectivos(efectivos)
                .permisosIndividuales(individuales)
                .build();
    }

    // ── Roles del usuario ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RolAsignadoDTO> getRolesUsuario(String uuid) {
        BeanUser usuario = findUsuario(uuid);
        return usuarioRolRepository.findAllByUsuario(usuario).stream()
                .map(ur -> RolAsignadoDTO.builder()
                        .idUsuarioRol(ur.getId())
                        .idRol(ur.getRol().getId())
                        .uuid(ur.getRol().getUuid())
                        .nombre(ur.getRol().getRole())
                        .fechaAsignacion(ur.getFechaAsignacion())
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("deprecation")
    @Transactional
    public RolAsignadoDTO asignarRol(String uuid, Long idRol) {
        BeanUser usuario = findUsuario(uuid);
        Role rol = roleRepository.findById(idRol)
                .orElseThrow(() -> new ObjNotFoundException("Rol no encontrado con id: " + idRol));

        if (ROOT_ROLE.equals(rol.getRole()) && !isCallerRoot()) {
            throw new ValidationException("No tiene permisos para asignar este rol");
        }

        if (usuarioRolRepository.existsByUsuarioAndRol(usuario, rol)) {
            throw new ObjConflictException("El usuario ya tiene el rol " + rol.getRole());
        }

        UsuarioRol ur = new UsuarioRol();
        ur.setUsuario(usuario);
        ur.setRol(rol);
        ur.setFechaAsignacion(LocalDateTime.now());
        UsuarioRol saved = usuarioRolRepository.save(ur);

        if (usuario.getRol() == null) {
            usuario.setRol(rol);
            userRepository.save(usuario);
        }

        registrarBitacora(uuid, "ROL_ASIGNADO",
                "Rol " + rol.getRole() + " asignado al usuario " + usuario.getUsername());

        return RolAsignadoDTO.builder()
                .idUsuarioRol(saved.getId())
                .idRol(rol.getId())
                .uuid(rol.getUuid())
                .nombre(rol.getRole())
                .fechaAsignacion(saved.getFechaAsignacion())
                .build();
    }

    @SuppressWarnings("deprecation")
    @Transactional
    public void quitarRol(String uuid, Long idRol) {
        BeanUser usuario = findUsuario(uuid);
        Role rol = roleRepository.findById(idRol)
                .orElseThrow(() -> new ObjNotFoundException("Rol no encontrado con id: " + idRol));

        if (ROOT_ROLE.equals(rol.getRole()) && !isCallerRoot()) {
            throw new ValidationException("No tiene permisos para modificar este rol");
        }

        if (!usuarioRolRepository.existsByUsuarioAndRol(usuario, rol)) {
            throw new ObjNotFoundException("El usuario no tiene el rol " + rol.getRole());
        }

        long totalRoles = usuarioRolRepository.countByUsuario(usuario);
        if (totalRoles <= 1) {
            throw new ValidationException("No se puede quitar el unico rol del usuario. Asigne otro rol antes de quitar este.");
        }

        usuarioRolRepository.deleteByUsuarioAndRol(usuario, rol);

        if (usuario.getRol() != null && usuario.getRol().getId().equals(rol.getId())) {
            List<UsuarioRol> restantes = usuarioRolRepository.findAllByUsuario(usuario);
            usuario.setRol(restantes.isEmpty() ? null : restantes.get(0).getRol());
            userRepository.save(usuario);
        }

        registrarBitacora(uuid, "ROL_REMOVIDO",
                "Rol " + rol.getRole() + " removido del usuario " + usuario.getUsername());
    }

    // ── Permisos individuales ───────────────────────────────────────────────────

    @Transactional
    public PermisoIndividualDTO concederPermiso(String uuid, PermisoIndividualRequestDTO request) {
        return crearPermisoIndividual(uuid, request, TipoPermisoIndividual.CONCESION);
    }

    @Transactional
    public PermisoIndividualDTO restringirPermiso(String uuid, PermisoIndividualRequestDTO request) {
        return crearPermisoIndividual(uuid, request, TipoPermisoIndividual.RESTRICCION);
    }

    @Transactional
    public void quitarPermisoIndividual(String uuid, Long idUsuarioPermiso) {
        findUsuario(uuid);
        UsuarioPermiso up = usuarioPermisoRepository.findById(idUsuarioPermiso)
                .orElseThrow(() -> new ObjNotFoundException("Permiso individual no encontrado con id: " + idUsuarioPermiso));

        if (!up.getUsuario().getUUID().equals(uuid)) {
            throw new ValidationException("El permiso individual no pertenece al usuario indicado");
        }

        up.setActivo(false);
        usuarioPermisoRepository.save(up);

        String accion = up.getTipo() == TipoPermisoIndividual.CONCESION
                ? "CONCESION_REVOCADA" : "RESTRICCION_REVOCADA";
        registrarBitacora(uuid, accion,
                up.getPermiso().getCodigo() + " - " + up.getTipo().name() + " revocada");
    }

    // ── Bitácora ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BitacoraPermisoDTO> getBitacora(String uuidUsuario, Pageable pageable) {
        if (uuidUsuario != null && !uuidUsuario.isBlank()) {
            return bitacoraRepository
                    .findAllByUsuarioAfectadoUuidOrderByTimestampDesc(uuidUsuario, pageable)
                    .map(this::toBitacoraDTO);
        }
        return bitacoraRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::toBitacoraDTO);
    }

    // ── Helpers privados ────────────────────────────────────────────────────────

    private PermisoIndividualDTO crearPermisoIndividual(String uuid, PermisoIndividualRequestDTO request,
                                                         TipoPermisoIndividual tipo) {
        BeanUser usuario = findUsuario(uuid);
        Permiso permiso = permisoRepository.findByCodigo(request.getCodigoPermiso())
                .orElseThrow(() -> new ObjNotFoundException("Permiso no encontrado: " + request.getCodigoPermiso()));

        BeanUser operador = institucionCtx.getUsuarioActual();

        UsuarioPermiso up = new UsuarioPermiso();
        up.setUsuario(usuario);
        up.setPermiso(permiso);
        up.setTipo(tipo);
        up.setMotivo(request.getMotivo());
        up.setFechaFin(request.getFechaFin());
        up.setOtorgadoPor(operador);
        up.setActivo(true);
        UsuarioPermiso saved = usuarioPermisoRepository.save(up);

        String accion = tipo == TipoPermisoIndividual.CONCESION
                ? "PERMISO_CONCEDIDO" : "PERMISO_RESTRINGIDO";
        String detalle = permiso.getCodigo();
        if (request.getFechaFin() != null) {
            detalle += " (temporal hasta " + request.getFechaFin() + ")";
        }
        if (request.getMotivo() != null) {
            detalle += " — " + request.getMotivo();
        }
        registrarBitacora(uuid, accion, detalle);

        return toPermisoIndividualDTO(saved);
    }

    private List<PermisoEfectivoDTO> buildPermisosEfectivos(BeanUser usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            return Collections.emptyList();
        }

        Map<String, Permiso> catalogoPermisos = permisoRepository.findAll().stream()
                .collect(Collectors.toMap(Permiso::getCodigo, p -> p));

        List<UsuarioPermiso> individuales = usuarioPermisoRepository
                .findAllByUsuarioAndActivoTrue(usuario);
        LocalDateTime ahora = LocalDateTime.now();

        Map<String, String> restricciones = new HashMap<>();
        Map<String, String> concesiones = new HashMap<>();

        for (UsuarioPermiso up : individuales) {
            if (!dentroDeVigencia(up, ahora)) continue;
            String codigo = up.getPermiso().getCodigo();
            if (up.getTipo() == TipoPermisoIndividual.RESTRICCION) {
                restricciones.put(codigo, up.getMotivo() != null ? up.getMotivo() : "");
            } else {
                String detalle = up.getFechaFin() != null ? "TEMPORAL" : "CONCESION_INDIVIDUAL";
                concesiones.put(codigo, detalle);
            }
        }

        Map<String, String> heredados = new HashMap<>();
        List<UsuarioRol> rolesUsuario = usuarioRolRepository.findAllByUsuario(usuario);
        for (UsuarioRol ur : rolesUsuario) {
            for (RolPermiso rp : rolPermisoRepository.findAllByRol(ur.getRol())) {
                heredados.putIfAbsent(rp.getPermiso().getCodigo(), ur.getRol().getRole());
            }
        }

        List<PermisoEfectivoDTO> resultado = new ArrayList<>();

        for (Map.Entry<String, String> entry : restricciones.entrySet()) {
            Permiso p = catalogoPermisos.get(entry.getKey());
            if (p == null) continue;
            resultado.add(PermisoEfectivoDTO.builder()
                    .codigo(p.getCodigo())
                    .modulo(p.getModulo())
                    .descripcion(p.getDescripcion())
                    .origen("RESTRICCION")
                    .detalleOrigen(entry.getValue())
                    .build());
        }

        for (Map.Entry<String, String> entry : concesiones.entrySet()) {
            if (restricciones.containsKey(entry.getKey())) continue;
            Permiso p = catalogoPermisos.get(entry.getKey());
            if (p == null) continue;
            resultado.add(PermisoEfectivoDTO.builder()
                    .codigo(p.getCodigo())
                    .modulo(p.getModulo())
                    .descripcion(p.getDescripcion())
                    .origen(entry.getValue())
                    .detalleOrigen(null)
                    .build());
        }

        for (Map.Entry<String, String> entry : heredados.entrySet()) {
            if (restricciones.containsKey(entry.getKey())) continue;
            if (concesiones.containsKey(entry.getKey())) continue;
            Permiso p = catalogoPermisos.get(entry.getKey());
            if (p == null) continue;
            resultado.add(PermisoEfectivoDTO.builder()
                    .codigo(p.getCodigo())
                    .modulo(p.getModulo())
                    .descripcion(p.getDescripcion())
                    .origen("ROL_HEREDADO")
                    .detalleOrigen(entry.getValue())
                    .build());
        }

        resultado.sort(Comparator.comparing(PermisoEfectivoDTO::getModulo)
                .thenComparing(PermisoEfectivoDTO::getCodigo));

        return resultado;
    }

    private BeanUser findUsuario(String uuid) {
        return userRepository.findByUUID(uuid)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado con UUID: " + uuid));
    }

    private void registrarBitacora(String uuidAfectado, String accion, String detalle) {
        BeanUser operador = null;
        try {
            operador = institucionCtx.getUsuarioActual();
        } catch (Exception ignored) {
        }

        BitacoraPermisos entrada = new BitacoraPermisos();
        entrada.setUsuarioAfectadoUuid(uuidAfectado);
        entrada.setAccion(accion);
        entrada.setDetalle(detalle);
        entrada.setRealizadoPorUuid(operador != null ? operador.getUUID() : "SISTEMA");
        bitacoraRepository.save(entrada);
    }

    private boolean dentroDeVigencia(UsuarioPermiso up, LocalDateTime ahora) {
        if (up.getFechaInicio() != null && ahora.isBefore(up.getFechaInicio())) return false;
        return up.getFechaFin() == null || !ahora.isAfter(up.getFechaFin());
    }

    private String buildNombre(BeanUser u) {
        if (u.getPersona() == null) return u.getUsername();
        StringBuilder sb = new StringBuilder(u.getPersona().getNombre());
        sb.append(" ").append(u.getPersona().getApellidoPaterno());
        if (u.getPersona().getApellidoMaterno() != null && !u.getPersona().getApellidoMaterno().isBlank()) {
            sb.append(" ").append(u.getPersona().getApellidoMaterno());
        }
        return sb.toString().trim();
    }

    private PermisoDTO toPermisoDTO(Permiso p) {
        return PermisoDTO.builder()
                .id(p.getId())
                .codigo(p.getCodigo())
                .modulo(p.getModulo())
                .descripcion(p.getDescripcion())
                .activo(p.getActivo())
                .build();
    }

    private PermisoIndividualDTO toPermisoIndividualDTO(UsuarioPermiso up) {
        return PermisoIndividualDTO.builder()
                .id(up.getId())
                .codigoPermiso(up.getPermiso().getCodigo())
                .modulo(up.getPermiso().getModulo())
                .descripcion(up.getPermiso().getDescripcion())
                .tipo(up.getTipo().name())
                .motivo(up.getMotivo())
                .fechaInicio(up.getFechaInicio())
                .fechaFin(up.getFechaFin())
                .otorgadoPorUuid(up.getOtorgadoPor() != null ? up.getOtorgadoPor().getUUID() : null)
                .otorgadoPorUsername(up.getOtorgadoPor() != null ? up.getOtorgadoPor().getUsername() : null)
                .fechaCreacion(up.getFechaCreacion())
                .activo(up.getActivo())
                .build();
    }

    private BitacoraPermisoDTO toBitacoraDTO(BitacoraPermisos b) {
        return BitacoraPermisoDTO.builder()
                .id(b.getId())
                .usuarioAfectadoUuid(b.getUsuarioAfectadoUuid())
                .accion(b.getAccion())
                .detalle(b.getDetalle())
                .realizadoPorUuid(b.getRealizadoPorUuid())
                .timestamp(b.getTimestamp())
                .build();
    }
}
