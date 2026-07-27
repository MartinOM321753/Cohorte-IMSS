package imss.gob.mx.cohorte.services.permisos;

import imss.gob.mx.cohorte.modules.permisos.*;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermisoEvaluationService {

    private static final String ROOT_ROLE = "ROOT";

    private final UsuarioRolRepository usuarioRolRepository;
    private final RolPermisoRepository rolPermisoRepository;
    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final PermisoRepository permisoRepository;

    @Transactional(readOnly = true)
    public Set<String> getPermisosEfectivos(BeanUser usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            return Collections.emptySet();
        }

        List<UsuarioRol> rolesUsuario = usuarioRolRepository.findAllByUsuario(usuario);

        boolean isRoot = rolesUsuario.stream()
                .anyMatch(ur -> ROOT_ROLE.equals(ur.getRol().getRole()));
        if (isRoot) {
            return permisoRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                    .map(Permiso::getCodigo)
                    .collect(Collectors.toSet());
        }

        List<UsuarioPermiso> individuales = usuarioPermisoRepository
                .findAllByUsuarioAndActivoTrue(usuario);

        LocalDateTime ahora = LocalDateTime.now();

        Set<String> restricciones = individuales.stream()
                .filter(up -> up.getTipo() == TipoPermisoIndividual.RESTRICCION)
                .filter(up -> dentroDeVigencia(up, ahora))
                .map(up -> up.getPermiso().getCodigo())
                .collect(Collectors.toSet());

        Set<String> concesiones = individuales.stream()
                .filter(up -> up.getTipo() == TipoPermisoIndividual.CONCESION)
                .filter(up -> dentroDeVigencia(up, ahora))
                .map(up -> up.getPermiso().getCodigo())
                .collect(Collectors.toSet());

        List<Role> roles = rolesUsuario.stream()
                .map(UsuarioRol::getRol)
                .collect(Collectors.toList());

        Set<String> heredados = Collections.emptySet();
        if (!roles.isEmpty()) {
            heredados = rolPermisoRepository.findAllByRolIn(roles).stream()
                    .map(rp -> rp.getPermiso().getCodigo())
                    .collect(Collectors.toSet());
        }

        Set<String> efectivos = new HashSet<>();
        efectivos.addAll(concesiones);

        for (String codigo : heredados) {
            if (!restricciones.contains(codigo)) {
                efectivos.add(codigo);
            }
        }

        for (String codigo : concesiones) {
            efectivos.add(codigo);
        }

        efectivos.removeAll(restricciones);

        return efectivos;
    }

    @Transactional(readOnly = true)
    public boolean tienePermiso(BeanUser usuario, String codigoPermiso) {
        return getPermisosEfectivos(usuario).contains(codigoPermiso);
    }

    @Transactional(readOnly = true)
    public List<String> getRoleNames(BeanUser usuario) {
        return usuarioRolRepository.findAllByUsuario(usuario).stream()
                .map(ur -> ur.getRol().getRole())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String getRolesAsString(BeanUser usuario) {
        List<String> roles = getRoleNames(usuario);
        return roles.isEmpty() ? "SIN_ROL" : String.join(",", roles);
    }

    private boolean dentroDeVigencia(UsuarioPermiso up, LocalDateTime ahora) {
        if (up.getFechaInicio() != null && ahora.isBefore(up.getFechaInicio())) {
            return false;
        }
        if (up.getFechaFin() != null && ahora.isAfter(up.getFechaFin())) {
            return false;
        }
        return true;
    }
}
