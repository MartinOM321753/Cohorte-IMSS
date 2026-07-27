package imss.gob.mx.cohorte.security.jwt;

import imss.gob.mx.cohorte.modules.permisos.UsuarioRol;
import imss.gob.mx.cohorte.modules.permisos.UsuarioRolRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.services.permisos.PermisoEvaluationService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class UDService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PermisoEvaluationService permisoEvaluationService;
    private final UsuarioRolRepository usuarioRolRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        BeanUser found = userRepository.findByUUID(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        if (!found.getActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }

        Set<String> permisos = permisoEvaluationService.getPermisosEfectivos(found);
        List<GrantedAuthority> authorities = new ArrayList<>();

        permisos.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

        usuarioRolRepository.findAllByUsuario(found).forEach(ur ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + ur.getRol().getRole())));

        return new org.springframework.security.core.userdetails.User(
                found.getUUID(),
                found.getPassword(),
                authorities
        );
    }
}
