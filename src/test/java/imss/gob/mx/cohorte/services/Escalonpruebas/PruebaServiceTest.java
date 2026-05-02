package imss.gob.mx.cohorte.services.Escalonpruebas;

import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalonRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PruebaServiceTest {

    @Mock
    private PruebaEscalonRepository pruebaRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PruebaService service;

    @Test
    void createFailsWhenUserDoesNotExist() {
        PruebaEscalon prueba = pruebaBase();
        when(pacienteRepository.findByUuid("paciente-1")).thenReturn(Optional.of(new Paciente()));
        when(userRepository.findByUUID("usuario-1")).thenReturn(Optional.empty());

        assertThrows(ObjNotFoundException.class, () -> service.create(prueba));
    }

    @Test
    void createRejectsInactiveUser() {
        PruebaEscalon prueba = pruebaBase();
        Paciente paciente = new Paciente();
        BeanUser usuario = new BeanUser();
        usuario.setActivo(false);
        when(pacienteRepository.findByUuid("paciente-1")).thenReturn(Optional.of(paciente));
        when(userRepository.findByUUID("usuario-1")).thenReturn(Optional.of(usuario));

        assertThrows(ObjConflictException.class, () -> service.create(prueba));
    }

    @Test
    void createUsesManagedPatientAndUser() {
        PruebaEscalon prueba = pruebaBase();
        Paciente paciente = new Paciente();
        BeanUser usuario = new BeanUser();
        usuario.setActivo(true);

        when(pacienteRepository.findByUuid("paciente-1")).thenReturn(Optional.of(paciente));
        when(userRepository.findByUUID("usuario-1")).thenReturn(Optional.of(usuario));
        when(pruebaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PruebaEscalon created = service.create(prueba);

        assertSame(paciente, created.getPaciente());
        assertSame(usuario, created.getUsuarioRealiza());
        assertNotNull(created.getFechaRegistro());
        assertNotNull(created.getFechaActualizacion());
    }

    private PruebaEscalon pruebaBase() {
        PruebaEscalon prueba = new PruebaEscalon();
        Paciente paciente = new Paciente();
        paciente.setUuid("paciente-1");
        prueba.setPaciente(paciente);
        BeanUser usuario = new BeanUser();
        usuario.setUUID("usuario-1");
        prueba.setUsuarioRealiza(usuario);
        prueba.setFechaEstudio(LocalDate.of(2026, 4, 20));
        return prueba;
    }
}
