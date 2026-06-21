package imss.gob.mx.cohorte.services.Personas;

import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.persona.PersonaRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;
    private static final Logger log = LoggerFactory.getLogger(PersonaService.class);


    public List<Persona> getAllPerson() {
        return personaRepository.findAll();
    }

    public Persona findPerson(Long idPersona) {
        return personaRepository.findById(idPersona)
                .orElseThrow(() -> new ObjNotFoundException("No se encontraron las personas"));
    }


    public Persona createPerson(Persona persona) {

        if (persona.getEmail() != null && !persona.getEmail().isBlank()
                && personaRepository.findByEmail(persona.getEmail()).isPresent()) {
            throw new ObjConflictException("El correo electrónico ya fue registrado");
        }

        if (persona.getTelefono() != null && !persona.getTelefono().isBlank()
                && personaRepository.findByTelefono(persona.getTelefono()).isPresent()) {
            throw new ObjConflictException("El número telefónico ya fue registrado");
        }

        persona.setFechaRegistro(LocalDateTime.now());
        persona.setFechaActualizacion(LocalDateTime.now());

        return personaRepository.save(persona);
    }


    public Persona update(Persona persona) {

        Persona personaBD = personaRepository.findById(persona.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la persona"));

        String nuevoEmail = persona.getEmail();
        if (nuevoEmail != null && !nuevoEmail.isBlank() && !nuevoEmail.equals(personaBD.getEmail())) {
            if (personaRepository.findByEmail(nuevoEmail).isPresent()) {
                throw new ObjConflictException("El correo electrónico ya fue registrado");
            }
            personaBD.setEmail(nuevoEmail);
        } else if (nuevoEmail == null || nuevoEmail.isBlank()) {
            personaBD.setEmail(nuevoEmail);
        }

        String nuevoTelefono = persona.getTelefono();
        if (nuevoTelefono != null && !nuevoTelefono.isBlank() && !nuevoTelefono.equals(personaBD.getTelefono())) {
            if (personaRepository.findByTelefono(nuevoTelefono).isPresent()) {
                throw new ObjConflictException("El número telefónico ya fue registrado");
            }
            personaBD.setTelefono(nuevoTelefono);
        } else if (nuevoTelefono == null || nuevoTelefono.isBlank()) {
            personaBD.setTelefono(nuevoTelefono);
        }
        personaBD.setNombre(persona.getNombre());
        personaBD.setApellidoPaterno(persona.getApellidoPaterno());
        personaBD.setApellidoMaterno(persona.getApellidoMaterno());
        personaBD.setFechaNacimiento(persona.getFechaNacimiento());
        personaBD.setSexo(persona.getSexo());


        personaBD.setFechaActualizacion(LocalDateTime.now());
        return personaRepository.save(personaBD);
    }
}
