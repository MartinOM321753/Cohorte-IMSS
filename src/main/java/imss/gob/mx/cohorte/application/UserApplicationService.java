package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class UserApplicationService {
    private final UserService userService;
    private final PersonaService personaService;
    private final RoleRepository roleRepository;


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
    @Transactional
    public BeanUser saveUser(BeanUser beanUser) {
        Persona savePersona = personaService.createPerson(beanUser.getPersona());
        Role findRole =  roleRepository.findByRole("USER").get();
        beanUser.setRol(findRole);
        beanUser.setPersona(savePersona);
        return userService.save(beanUser);
    }
    @Transactional
    public BeanUser updateUser(BeanUser beanUser) {
        Persona updatePersona =  personaService.createPerson(beanUser.getPersona());
        beanUser.setPersona(updatePersona);
        return userService.updateUser(beanUser);
    }
    @Transactional
    public BeanUser onlySaveUser(BeanUser beanUser) {
        return userService.save(beanUser);
    }
    @Transactional
    public BeanUser onlyUpdateUser(BeanUser beanUser) {
        return userService.updateUser(beanUser);
    }




}
