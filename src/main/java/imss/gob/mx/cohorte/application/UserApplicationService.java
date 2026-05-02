package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
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
        Role findRole = roleRepository.findById(beanUser.getRol().getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el rol solicitado"));
        beanUser.setRol(findRole);
        beanUser.setPersona(savePersona);
        return userService.save(beanUser);
    }

    @Transactional
    public BeanUser updateUser(BeanUser beanUser) {
        BeanUser existing = userService.getUser(beanUser.getId());
        beanUser.getPersona().setId(existing.getPersona().getId());
        Persona updatePersona = personaService.update(beanUser.getPersona());
        Role updatedRole = roleRepository.findById(beanUser.getRol().getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el rol solicitado"));

        beanUser.setPersona(updatePersona);
        beanUser.setRol(updatedRole);
        beanUser.setActivo(existing.getActivo());
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
