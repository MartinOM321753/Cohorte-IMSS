package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PacienteApplicationServiceTest {

    @Mock
    private PacienteService pacienteService;
    @Mock
    private PersonaService personaService;

    @InjectMocks
    private PacienteApplicationService service;

    @Test
    void updateUserUsesPersonaUpdate() {
        Paciente existing = new Paciente();
        existing.setId(1L);
        Persona existingPersona = new Persona();
        existingPersona.setId(44L);
        existing.setPersona(existingPersona);

        Paciente incoming = new Paciente();
        incoming.setId(1L);
        incoming.setFolio("F-1");
        Persona incomingPersona = new Persona();
        incoming.setPersona(incomingPersona);

        Persona updatedPersona = new Persona();
        updatedPersona.setId(44L);

        when(pacienteService.getPatient(1L)).thenReturn(existing);
        when(personaService.update(any())).thenReturn(updatedPersona);
        when(pacienteService.updatePatient(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Paciente updated = service.updateUser(incoming);

        assertEquals(44L, incoming.getPersona().getId());
        assertEquals(updatedPersona, updated.getPersona());
    }
}
