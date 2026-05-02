package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PersonaService personaService;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserApplicationService service;

    @Test
    void updateUserUsesPersonaUpdateAndResolvedRole() {
        BeanUser existing = new BeanUser();
        existing.setId(1L);
        existing.setActivo(true);
        Persona existingPersona = new Persona();
        existingPersona.setId(30L);
        existing.setPersona(existingPersona);

        BeanUser incoming = new BeanUser();
        incoming.setId(1L);
        incoming.setUsername("nuevo");
        incoming.setPassword("password123");
        Persona incomingPersona = new Persona();
        incoming.setPersona(incomingPersona);
        Role requestedRole = new Role();
        requestedRole.setId(2L);
        incoming.setRol(requestedRole);

        Persona updatedPersona = new Persona();
        updatedPersona.setId(30L);
        Role updatedRole = new Role();
        updatedRole.setId(2L);
        updatedRole.setRole("USER");

        when(userService.getUser(1L)).thenReturn(existing);
        when(personaService.update(any())).thenReturn(updatedPersona);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(updatedRole));
        when(userService.updateUser(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BeanUser updated = service.updateUser(incoming);

        assertEquals(30L, incoming.getPersona().getId());
        assertEquals(updatedPersona, updated.getPersona());
        assertEquals(updatedRole, updated.getRol());
        assertEquals(true, updated.getActivo());
    }
}
