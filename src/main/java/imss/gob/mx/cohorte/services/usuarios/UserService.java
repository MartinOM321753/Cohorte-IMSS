package imss.gob.mx.cohorte.services.usuarios;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.PasswordEncoder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<BeanUser> getAllUser(){
        return userRepository.findAll();
    }


    public BeanUser getUser(Long idUser){
        return userRepository.findById(idUser).orElseThrow(()-> new ObjNotFoundException("No se encontro el usuario"));
    }
    public List<BeanUser> getAllUserByStatus(Boolean status){
        return userRepository.findAllByActivo(status);
    }

    public BeanUser getByUUID(String idUser){
        return userRepository.findByUUID(idUser).orElseThrow(()-> new ObjNotFoundException("No se encontro el usuario"));
    }
    public Boolean existsByUUID (String idUser){
        return userRepository.existsByUUID(idUser);
    }



    public BeanUser save(BeanUser beanUser) {

        if (userRepository.findByUsername(beanUser.getUsername()).isPresent()) {
            throw new ObjConflictException("El nombre de usuario ya existe");
        }
        beanUser.setPassword(PasswordEncoder.encodePassword(beanUser.getPassword()));
        beanUser.setFechaCreacion(LocalDateTime.now());
        beanUser.setFechaActualizacion(LocalDateTime.now());

        return userRepository.save(beanUser);
    }

    public BeanUser updateUser(BeanUser beanUser) {

        BeanUser beanUserBD = userRepository.findById(beanUser.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el usuario"));

        if (!beanUser.getUsername().equals(beanUserBD.getUsername())) {
            if (userRepository.findByUsername(beanUser.getUsername()).isPresent()) {
                throw new ObjConflictException("El nombre de usuario ya existe");
            }
            beanUserBD.setUsername(beanUser.getUsername());
        }
        if (!PasswordEncoder.verifyPassword(beanUser.getPassword(), beanUserBD.getPassword()) ) {
            beanUserBD.setPassword(PasswordEncoder.encodePassword(beanUser.getPassword()));
        }

        beanUserBD.setActivo(beanUser.getActivo());
        beanUserBD.setFechaActualizacion(LocalDateTime.now());

        return userRepository.save(beanUserBD);
    }








}
