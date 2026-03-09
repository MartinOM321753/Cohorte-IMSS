package imss.gob.mx.cohorte.services.Personas;

import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.persona.PersonaRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
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

        if (personaRepository.findByEmail(persona.getEmail()).isPresent()) {
            throw new ObjConflictException("El correo ya existe");
        }

        if (personaRepository.findByTelefono(persona.getTelefono()).isPresent()) {
            throw new ObjConflictException("El teléfono ya existe");
        }

        persona.setFechaRegistro(LocalDateTime.now());
        persona.setFechaActualizacion(LocalDateTime.now());

        return personaRepository.save(persona);
    }


    public Persona update(Persona persona) {

        Persona personaBD = personaRepository.findById(persona.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la persona"));

        if (!persona.getEmail().equals(personaBD.getEmail())) {
            if (personaRepository.findByEmail(persona.getEmail()).isPresent()) {
                throw new ObjConflictException("El correo ya existe");
            }
            personaBD.setEmail(persona.getEmail());
        }

        if (!persona.getTelefono().equals(personaBD.getTelefono())) {
            if (personaRepository.findByTelefono(persona.getTelefono()).isPresent()) {
                throw new ObjConflictException("El teléfono ya existe");
            }
            personaBD.setTelefono(persona.getTelefono());
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
