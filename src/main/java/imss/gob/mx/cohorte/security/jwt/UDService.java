package imss.gob.mx.cohorte.security.jwt;

import imss.gob.mx.cohorte.models.user.User;
import imss.gob.mx.cohorte.models.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UDService  implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User found =userRepository.findByUsername(username).orElse(null);
        if (found == null) {
            throw new UsernameNotFoundException("No se encontro ese usuarios con el nombre : " + username);
        }

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + found.getRole().getName());

            return new org.springframework.security.core.userdetails.User(
                    found.getUsername(),
                    found.getPassword(),
                    Collections.singleton(authority)
            );
        }
    }
