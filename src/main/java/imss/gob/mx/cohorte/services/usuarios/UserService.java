package imss.gob.mx.cohorte.services.usuarios;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
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
        if (!findUser.getActivo()) {
            throw new ObjNotFoundException("El usuario no esta activo");
        }
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
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el usuario"));

        // El username es inmutable: se generó automáticamente al crear el usuario
        // y no se modifica en ediciones posteriores. Solo validar si viene explícito.
        if (beanUser.getUsername() != null && !beanUser.getUsername().isBlank()
                && !beanUser.getUsername().equals(beanUserBD.getUsername())
                && userRepository.findByUsername(beanUser.getUsername()).isPresent()) {
            throw new ObjConflictException("El nombre de usuario ya existe");
        }
        // Solo re-encriptar si se proporcionó nueva contraseña (en edición normal no se toca)
        if (beanUser.getPassword() != null && !beanUser.getPassword().isBlank()) {
            if (!passwordEncoder.matches(beanUser.getPassword(), beanUserBD.getPassword())) {
                beanUserBD.setPassword(passwordEncoder.encode(beanUser.getPassword()));
            }
        }
        beanUserBD.setPersona(beanUser.getPersona());
        beanUserBD.setRol(beanUser.getRol());
        if (beanUser.getActivo() != null) {
            beanUserBD.setActivo(beanUser.getActivo());
        }
        beanUserBD.setFechaActualizacion(LocalDateTime.now());

        return userRepository.save(beanUserBD);
    }

    /**
     * Activa o desactiva un usuario sin tocar ningún otro campo.
     */
    @Transactional
    public BeanUser setActivo(Long id, Boolean activo) {
        BeanUser user = userRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el usuario"));
        user.setActivo(activo);
        user.setFechaActualizacion(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Cambia la contraseña del usuario identificado por UUID.
     * Si el usuario tiene debeResetear=true (contraseña generada por sistema),
     * no se valida la contraseña actual. De lo contrario, es obligatoria.
     */
    @Transactional
    public void cambiarPassword(String uuid, String passwordActual, String nuevaPassword) {
        BeanUser user = userRepository.findByUUID(uuid)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado"));

        boolean esPrimerCambio = Boolean.TRUE.equals(user.getDebeResetear());

        if (!esPrimerCambio) {
            if (passwordActual == null || passwordActual.isBlank()) {
                throw new ValidationException("La contraseña actual es requerida");
            }
            if (!passwordEncoder.matches(passwordActual, user.getPassword())) {
                throw new ValidationException("La contraseña actual es incorrecta");
            }
        }

        user.setPassword(passwordEncoder.encode(nuevaPassword));
        user.setDebeResetear(false);
        user.setFechaActualizacion(LocalDateTime.now());
        userRepository.save(user);
    }
}
