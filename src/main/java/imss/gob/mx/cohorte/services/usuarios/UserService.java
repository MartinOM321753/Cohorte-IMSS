package imss.gob.mx.cohorte.services.usuarios;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<BeanUser> getAllUser() {
        return userRepository.findAll();
    }

    public BeanUser getUser(Long idUser) {
        return userRepository.findById(idUser)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el usuario"));
    }

    public List<BeanUser> getAllUserByStatus(Boolean status) {
        return userRepository.findAllByActivo(status);
    }

    public BeanUser getByUUID(String idUser) {
        BeanUser findUser = userRepository.findByUUID(idUser)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el usuario"));
        if (!findUser.getActivo()) throw new ObjNotFoundException("El usuario no esta activo");
        return findUser;
    }

    public Boolean existsByUUID(String idUser) {
        return userRepository.existsByUUID(idUser);
    }

    public Optional<BeanUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public BeanUser save(BeanUser beanUser) {

        if (userRepository.findByUsername(beanUser.getUsername()).isPresent()) {
            throw new ObjConflictException("El nombre de usuario ya existe");
        }


        beanUser.setPassword(passwordEncoder.encode(beanUser.getPassword()));
        beanUser.setFechaCreacion(LocalDateTime.now());
        beanUser.setFechaActualizacion(LocalDateTime.now());
        return userRepository.save(beanUser);
    }

    @Transactional
    public BeanUser updateUser(BeanUser beanUser) {
        BeanUser beanUserBD = userRepository.findById(beanUser.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el usuario"));

        if (!beanUser.getUsername().equals(beanUserBD.getUsername())) {
            if (userRepository.findByUsername(beanUser.getUsername()).isPresent()) {
                throw new ObjConflictException("El nombre de usuario ya existe");
            }
            beanUserBD.setUsername(beanUser.getUsername());
        }
        if (!passwordEncoder.matches(beanUser.getPassword(), beanUserBD.getPassword())) {
            beanUserBD.setPassword(passwordEncoder.encode(beanUser.getPassword()));
        }

        beanUserBD.setActivo(beanUser.getActivo());
        beanUserBD.setFechaActualizacion(LocalDateTime.now());

        return userRepository.save(beanUserBD);
    }
}
